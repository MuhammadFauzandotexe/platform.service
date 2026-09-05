package mdro.platform.service.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import mdro.platform.service.entity.WhatsAppSession;

public interface WhatsAppSessionRepository extends JpaRepository<WhatsAppSession, UUID> {
}
