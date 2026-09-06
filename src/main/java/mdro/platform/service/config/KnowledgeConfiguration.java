package mdro.platform.service.config;

import mdro.platform.service.service.knowledge.KnowledgeChunkingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KnowledgeChunkingProperties.class)
public class KnowledgeConfiguration {
}
