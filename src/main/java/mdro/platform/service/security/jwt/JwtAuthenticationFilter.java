package mdro.platform.service.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import mdro.platform.service.security.principal.AccountPrincipal;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.core.AuthenticationException("Invalid bearer token") {
                    });
            return;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.core.AuthenticationException("Invalid bearer token") {
                    });
            return;
        }

        var principal = jwtService.parsePrincipal(token);
        if (principal.isEmpty()) {
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.core.AuthenticationException("Invalid bearer token") {
                    });
            return;
        }

        AccountPrincipal accountPrincipal = principal.get();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(accountPrincipal, null, List.of())
        );
        filterChain.doFilter(request, response);
    }
}
