package mdro.platform.service.integration.qdrant.exception;

public class QdrantIntegrationException extends QdrantException {

    public QdrantIntegrationException(String message) {
        super(message);
    }

    public QdrantIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public QdrantIntegrationException(String message, Integer httpStatusCode) {
        super(message, httpStatusCode);
    }
}
