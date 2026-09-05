package mdro.platform.service.repository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import mdro.platform.service.entity.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByEmail(String email);

    Optional<Account> findByEmail(String email);
}
