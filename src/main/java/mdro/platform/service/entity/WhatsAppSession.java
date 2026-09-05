package mdro.platform.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mdro.platform.service.model.whatsapp.WhatsAppSessionStatus;

@Entity
@Table(name = "whatsapp_sessions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_whatsapp_sessions_session_id", columnNames = "session_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhatsAppSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "session_name")
    private String sessionName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WhatsAppSessionStatus status;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "connected_at")
    private OffsetDateTime connectedAt;

    @Column(name = "disconnected_at")
    private OffsetDateTime disconnectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public WhatsAppSession(UUID sessionId, String sessionName, Account account) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.account = account;
        this.status = WhatsAppSessionStatus.CREATING;
    }

    public void markGatewayResponse(
            WhatsAppSessionStatus status,
            OffsetDateTime expiresAt,
            OffsetDateTime connectedAt) {
        this.status = status;
        this.expiresAt = expiresAt;
        this.connectedAt = connectedAt;
    }

    public void markFailed() {
        this.status = WhatsAppSessionStatus.FAILED;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
