package mdro.platform.service.exception.whatsapp;

public class WhatsAppSessionNotFoundException extends RuntimeException {

    public WhatsAppSessionNotFoundException(String sessionName) {
        super("No WhatsApp session found for session name: " + sessionName);
    }
}
