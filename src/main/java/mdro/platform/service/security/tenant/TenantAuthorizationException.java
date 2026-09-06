package mdro.platform.service.security.tenant;

public class TenantAuthorizationException extends RuntimeException {

    public TenantAuthorizationException(String message) {
        super(message);
    }
}
