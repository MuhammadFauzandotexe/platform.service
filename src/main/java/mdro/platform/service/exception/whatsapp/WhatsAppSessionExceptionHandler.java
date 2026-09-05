package mdro.platform.service.exception.whatsapp;

import java.util.LinkedHashMap;
import java.util.Map;
import mdro.platform.service.integration.whatsapp.exception.WhatsAppGatewayException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WhatsAppSessionExceptionHandler {

    @ExceptionHandler(WhatsAppSessionAccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAccountNotFound(
            WhatsAppSessionAccountNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Account not found", exception.getMessage());
    }

    @ExceptionHandler(WhatsAppGatewayException.class)
    public ResponseEntity<Map<String, String>> handleGatewayFailure(
            WhatsAppGatewayException exception) {
        String message = exception.getGatewayError() == null
                ? "WhatsApp Gateway communication failed"
                : exception.getGatewayError();
        return error(HttpStatus.BAD_GATEWAY, "WhatsApp Gateway error", message);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String error, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
