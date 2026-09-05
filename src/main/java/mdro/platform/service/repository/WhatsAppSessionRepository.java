package mdro.platform.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import mdro.platform.service.entity.WhatsAppSession;
import mdro.platform.service.model.whatsapp.WhatsAppSessionStatus;

public interface WhatsAppSessionRepository extends JpaRepository<WhatsAppSession, UUID> {

    List<WhatsAppSession> findAllByAccount_Id(UUID accountId);

    List<WhatsAppSession> findAllByAccount_IdAndSessionName(UUID accountId, String sessionName);

    List<WhatsAppSession> findAllByAccount_IdAndSessionNameAndIdNot(
            UUID accountId,
            String sessionName,
            UUID id);

    boolean existsByAccount_IdAndSessionNameAndStatus(
            UUID accountId,
            String sessionName,
            WhatsAppSessionStatus status);
}
