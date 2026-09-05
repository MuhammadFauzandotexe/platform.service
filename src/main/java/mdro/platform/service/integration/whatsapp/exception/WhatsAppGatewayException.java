package mdro.platform.service.integration.whatsapp.exception;

import lombok.Getter;

@Getter
public class WhatsAppGatewayException extends RuntimeException {

    private final Integer httpStatusCode;
    private final String gatewayError;

    public WhatsAppGatewayException(Integer httpStatusCode, String gatewayError) {
        super(buildMessage(httpStatusCode, gatewayError));
        this.httpStatusCode = httpStatusCode;
        this.gatewayError = gatewayError;
    }

    public WhatsAppGatewayException(Integer httpStatusCode, String gatewayError, Throwable cause) {
        super(buildMessage(httpStatusCode, gatewayError), cause);
        this.httpStatusCode = httpStatusCode;
        this.gatewayError = gatewayError;
    }

    private static String buildMessage(Integer httpStatusCode, String gatewayError) {
        String status = httpStatusCode == null ? "unavailable" : httpStatusCode.toString();
        return "WhatsApp Gateway communication failed (HTTP " + status
                + (gatewayError == null || gatewayError.isBlank() ? ")" : ", error: " + gatewayError + ")");
    }
}
