package mdro.platform.service.integration.whatsapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "whatsapp.gateway")
public class WhatsAppGatewayProperties {

    private String baseUrl;
}
