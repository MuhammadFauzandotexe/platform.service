package mdro.platform.service.exception.whatsapp;

import java.util.UUID;

public class WhatsAppSessionAccountNotFoundException extends RuntimeException {

    public WhatsAppSessionAccountNotFoundException(UUID accountId) {
        super("Authenticated account was not found: " + accountId);
    }
}
