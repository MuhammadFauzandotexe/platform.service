package mdro.platform.service.service.account;

import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import mdro.platform.service.entity.Account;
import mdro.platform.service.exception.account.DuplicateEmailException;
import mdro.platform.service.model.account.AccountPlan;
import mdro.platform.service.model.account.AccountStatus;
import mdro.platform.service.repository.AccountRepository;

@Service
@RequiredArgsConstructor
public class AccountRegistrationService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Account register(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        Account account = new Account(
                normalizedEmail,
                passwordEncoder.encode(password),
                AccountStatus.REGISTERED,
                AccountPlan.FREE
        );

        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException(normalizedEmail);
        }
    }
}
