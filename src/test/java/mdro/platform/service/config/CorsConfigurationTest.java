package mdro.platform.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigurationTest {

    @Test
    void createsConfiguredCorsPolicy() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        properties.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        properties.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        properties.setExposedHeaders(List.of());
        properties.setAllowCredentials(false);
        properties.setMaxAge(3600);

        CorsConfigurationSource source = new CorsConfiguration(properties).corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/whatsapp/sessions");

        org.springframework.web.cors.CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertEquals(properties.getAllowedOrigins(), configuration.getAllowedOrigins());
        assertEquals(properties.getAllowedMethods(), configuration.getAllowedMethods());
        assertEquals(properties.getAllowedHeaders(), configuration.getAllowedHeaders());
        assertEquals(List.of(), configuration.getExposedHeaders());
        assertFalse(configuration.getAllowCredentials());
        assertEquals(3600L, configuration.getMaxAge());
        assertTrue(configuration.checkOrigin("http://localhost:3000") != null);
        assertTrue(configuration.checkOrigin("http://untrusted.example") == null);
    }
}
