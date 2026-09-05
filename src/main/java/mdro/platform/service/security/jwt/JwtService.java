package mdro.platform.service.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import mdro.platform.service.model.account.AccountPlan;
import mdro.platform.service.model.account.AccountStatus;
import mdro.platform.service.security.principal.AccountPrincipal;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    public String generateToken(AccountPrincipal principal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.getExpiration());

        return Jwts.builder()
                .subject(principal.accountId().toString())
                .claim("email", principal.email())
                .claim("accountStatus", principal.accountStatus().name())
                .claim("accountPlan", principal.accountPlan().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    public Optional<AccountPrincipal> parsePrincipal(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.get("email", String.class);
            String status = claims.get("accountStatus", String.class);
            String plan = claims.get("accountPlan", String.class);
            if (email == null || email.isBlank() || status == null || plan == null) {
                throw new IllegalArgumentException("Required JWT claim is missing");
            }

            UUID accountId = UUID.fromString(claims.getSubject());
            return Optional.of(new AccountPrincipal(
                    accountId,
                    email,
                    Enum.valueOf(AccountStatus.class, status),
                    Enum.valueOf(AccountPlan.class, plan)
            ));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
