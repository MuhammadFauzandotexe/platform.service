package mdro.platform.service.integration.ollama.exception;

public class OllamaEmbeddingException extends RuntimeException {

    private final Integer httpStatusCode;

    public OllamaEmbeddingException(String message) {
        super(message);
        this.httpStatusCode = null;
    }

    public OllamaEmbeddingException(String message, Integer httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public OllamaEmbeddingException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = null;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}
