package mdro.platform.service.exception.whatsapp;

public class WhatsAppSessionAlreadyExistsException extends RuntimeException {

    public WhatsAppSessionAlreadyExistsException(String sessionName) {
        super("A connected WhatsApp session already exists for session name: " + sessionName);
    }
}
