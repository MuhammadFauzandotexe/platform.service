package mdro.platform.service.integration.qdrant.exception;

public class QdrantException extends RuntimeException {

    private final Integer httpStatusCode;

    public QdrantException(String message) {
        super(message);
        this.httpStatusCode = null;
    }

    public QdrantException(String message, Integer httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public QdrantException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = null;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}
