package mdro.platform.service.exception.knowledge;

public class KnowledgeNotFoundException extends RuntimeException {

    public KnowledgeNotFoundException() {
        super("Knowledge was not found");
    }
}
