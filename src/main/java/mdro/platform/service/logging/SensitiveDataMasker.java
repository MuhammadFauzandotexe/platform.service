package mdro.platform.service.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SensitiveDataMasker {

    private static final String MASK = "***";
    private static final Set<String> SENSITIVE_NAMES = Set.of(
            "password", "passwordhash", "authorization", "cookie", "setcookie",
            "accesstoken", "refreshtoken", "token", "jwt", "secret", "credential"
    );

    private final ObjectMapper objectMapper;

    public Map<String, String> maskHeaders(Map<String, String> headers) {
        Map<String, String> masked = new LinkedHashMap<>();
        headers.forEach((name, value) -> masked.put(name, isSensitive(name) ? MASK : value));
        return masked;
    }

    public Map<String, Object> maskParameters(Map<String, String[]> parameters) {
        Map<String, Object> masked = new LinkedHashMap<>();
        parameters.forEach((name, values) -> masked.put(
                name,
                isSensitive(name) ? MASK : values.length == 1 ? values[0] : values.clone()
        ));
        return masked;
    }

    public Object maskJson(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            maskNode(root);
            return root;
        } catch (Exception exception) {
            return "<invalid-json-body>";
        }
    }

    public boolean isSensitive(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replace("-", "");
        return SENSITIVE_NAMES.contains(normalized);
    }

    private void maskNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode value = objectNode.get(fieldName);
                if (isSensitive(fieldName)) {
                    objectNode.put(fieldName, MASK);
                } else {
                    maskNode(value);
                }
            });
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::maskNode);
        }
    }
}
