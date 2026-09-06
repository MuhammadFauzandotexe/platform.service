package mdro.platform.service.exception.knowledge;

import java.util.LinkedHashMap;
import java.util.Map;
import mdro.platform.service.controller.knowledge.KnowledgeController;
import mdro.platform.service.security.tenant.TenantAuthorizationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = KnowledgeController.class)
public class KnowledgeExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Request is invalid");
        return error(HttpStatus.BAD_REQUEST, "Invalid request", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedRequest() {
        return error(HttpStatus.BAD_REQUEST, "Invalid request", "Request body is invalid");
    }

    @ExceptionHandler(KnowledgeIngestionException.class)
    public ResponseEntity<Map<String, Object>> handleIngestion(KnowledgeIngestionException exception) {
        return error(HttpStatus.BAD_GATEWAY, "Knowledge ingestion failed", exception.getMessage());
    }

    @ExceptionHandler(KnowledgeQueryException.class)
    public ResponseEntity<Map<String, Object>> handleQuery() {
        return error(HttpStatus.BAD_GATEWAY, "Knowledge query failed",
                "Knowledge query could not be completed");
    }

    @ExceptionHandler(TenantAuthorizationException.class)
    public ResponseEntity<Map<String, Object>> handleTenantAuthorization() {
        return error(HttpStatus.FORBIDDEN, "Forbidden", "Authenticated account is not authorized");
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String error,
            String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
