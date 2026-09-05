package mdro.platform.service.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "whatsapp.session")
public class WhatsAppSessionConfiguration {

    private Duration pendingExpiration = Duration.ofMinutes(30);
}
