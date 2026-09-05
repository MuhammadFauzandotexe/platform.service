package mdro.platform.service.logging;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "logging.http")
public class HttpLoggingProperties {

    private boolean enabled = true;
    private boolean logRequestBody = true;
    private boolean logResponseBody = true;
    private int maxBodySize = 10000;
    private List<String> excludedPaths = new ArrayList<>();
}
