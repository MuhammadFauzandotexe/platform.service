package mdro.platform.service.service.account;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import mdro.platform.service.dto.account.response.AccountInfoResponse;
import mdro.platform.service.repository.AccountRepository;

@Service
@RequiredArgsConstructor
public class AccountInfoService {

    private final AccountRepository accountRepository;

    public Optional<AccountInfoResponse> findById(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(account -> new AccountInfoResponse(
                        account.getAccountStatus().name(),
                        account.getCreatedAt(),
                        account.getAccountPlan().name(),
                        account.getId(),
                        account.getEmail(),
                        account.getUpdatedAt()
                ));
    }
}
