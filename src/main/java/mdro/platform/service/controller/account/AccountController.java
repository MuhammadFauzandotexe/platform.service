package mdro.platform.service.controller.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import mdro.platform.service.dto.account.request.AccountRegistrationRequest;
import mdro.platform.service.dto.account.response.AccountInfoResponse;
import mdro.platform.service.dto.account.response.AccountRegistrationResponse;
import mdro.platform.service.entity.Account;
import mdro.platform.service.security.principal.AccountPrincipal;
import mdro.platform.service.service.account.AccountInfoService;
import mdro.platform.service.service.account.AccountRegistrationService;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final String SUCCESS_MESSAGE =
            "Account created successfully. Verification is required before the account becomes fully verified.";

    private final AccountRegistrationService registrationService;
    private final AccountInfoService accountInfoService;

    @PostMapping
    public ResponseEntity<AccountRegistrationResponse> register(
            @Valid @RequestBody AccountRegistrationRequest request) {
        Account account = registrationService.register(request.email(), request.password());
        AccountRegistrationResponse response = new AccountRegistrationResponse(
                account.getId(),
                account.getEmail(),
                account.getAccountStatus(),
                account.getAccountPlan(),
                SUCCESS_MESSAGE
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountInfoResponse> getAccountInfo(
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ResponseEntity.of(accountInfoService.findById(principal.accountId()));
    }
}
