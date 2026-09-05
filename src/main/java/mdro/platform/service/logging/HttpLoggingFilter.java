package mdro.platform.service.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
@EnableConfigurationProperties(HttpLoggingProperties.class)
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpLoggingFilter.class);
    private static final String JSON_CONTENT_TYPE = "json";
    private static final String EVENT_REQUEST = "http.request";
    private static final String EVENT_RESPONSE = "http.response";

    private final HttpLoggingProperties properties;
    private final SensitiveDataMasker masker;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || isExcluded(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request, properties.getMaxBodySize());
        BoundedResponseWrapper responseWrapper =
                new BoundedResponseWrapper(response, properties.getMaxBodySize());
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            try {
                logRequest(requestWrapper);
                logResponse(requestWrapper, responseWrapper, elapsedMillis(startedAt));
            } catch (RuntimeException loggingFailure) {
                LOGGER.warn("HTTP logging failed without affecting the response", loggingFailure);
            }
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("method", request.getMethod());
        event.put("path", request.getRequestURI());
        event.put("query", masker.maskParameters(request.getParameterMap()));
        event.put("headers", masker.maskHeaders(headers(request)));
        if (properties.isLogRequestBody()) {
            event.put("body", body(request.getContentAsByteArray(), request.getContentType(),
                    request.getCharacterEncoding(), request.getContentAsByteArray().length >= properties.getMaxBodySize()));
        }
        LOGGER.atInfo().addKeyValue("event", EVENT_REQUEST).addKeyValue("http", event).log("HTTP request");
    }

    private void logResponse(
            ContentCachingRequestWrapper request,
            BoundedResponseWrapper response,
            long durationMillis) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("method", request.getMethod());
        event.put("path", request.getRequestURI());
        event.put("status", response.getStatus());
        event.put("headers", masker.maskHeaders(headers(response)));
        event.put("durationMs", durationMillis);
        if (properties.isLogResponseBody()) {
            event.put("body", body(response.getCapturedBody(), response.getContentType(),
                    response.getCharacterEncoding(), response.isBodyTruncated()));
        }
        LOGGER.atInfo().addKeyValue("event", EVENT_RESPONSE).addKeyValue("http", event).log("HTTP response");
    }

    private Object body(byte[] bytes, String contentType, String encoding, boolean truncated) {
        if (bytes.length == 0) {
            return null;
        }
        if (contentType == null || !contentType.toLowerCase().contains(JSON_CONTENT_TYPE)) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("logged", false);
            metadata.put("contentType", contentType);
            metadata.put("size", bytes.length);
            metadata.put("truncated", truncated);
            return metadata;
        }
        String value = new String(bytes, encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding));
        Object masked = masker.maskJson(value);
        if (truncated && masked instanceof String) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("logged", false);
            metadata.put("contentType", contentType);
            metadata.put("size", bytes.length);
            metadata.put("truncated", true);
            return metadata;
        }
        return masked;
    }

    private Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, request.getHeader(name)));
        return headers;
    }

    private Map<String, String> headers(HttpServletResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        response.getHeaderNames().forEach(name -> headers.put(name, response.getHeader(name)));
        return headers;
    }

    private boolean isExcluded(String path) {
        return properties.getExcludedPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
