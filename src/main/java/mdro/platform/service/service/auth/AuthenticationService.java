package mdro.platform.service.service.auth;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import mdro.platform.service.entity.Account;
import mdro.platform.service.exception.auth.InvalidCredentialsException;
import mdro.platform.service.repository.AccountRepository;
import mdro.platform.service.security.jwt.JwtService;
import mdro.platform.service.security.jwt.JwtProperties;
import mdro.platform.service.security.principal.AccountPrincipal;
import mdro.platform.service.dto.auth.request.LoginRequest;
import mdro.platform.service.dto.auth.response.LoginResponse;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public LoginResponse authenticate(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        AccountPrincipal principal = new AccountPrincipal(
                account.getId(),
                account.getEmail(),
                account.getAccountStatus(),
                account.getAccountPlan()
        );

        return new LoginResponse(
                jwtService.generateToken(principal),
                "Bearer",
                jwtProperties.getExpiration(),
                new LoginResponse.AuthenticatedAccount(
                        account.getId(),
                        account.getEmail(),
                        account.getAccountStatus(),
                        account.getAccountPlan()
                )
        );
    }
}
