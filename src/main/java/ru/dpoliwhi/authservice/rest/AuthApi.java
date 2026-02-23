package ru.dpoliwhi.authservice.rest;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.dpoliwhi.authservice.dto.LoginRequest;
import ru.dpoliwhi.authservice.dto.LoginResponse;
import ru.dpoliwhi.authservice.dto.RegistrationRequest;
import ru.dpoliwhi.authservice.dto.TokensDto;
import ru.dpoliwhi.authservice.service.KeycloakUserService;
import ru.dpoliwhi.authservice.util.CookieUtil;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthApi {

    private final KeycloakUserService keycloakUserService;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @PostMapping("/registration")
    public ResponseEntity<Void> register(@RequestBody RegistrationRequest request) {
        log.info("Registration request received for user: {}", request.getUsername());
        keycloakUserService.registerUser(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/registration/check")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        log.debug("Checking username availability: {}", username);
        boolean taken = keycloakUserService.isUsernameTaken(username);
        return ResponseEntity.ok(taken);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        log.info("Login attempt for user: {}", request.getUsername());
        TokensDto tokens = keycloakUserService.login(request.getUsername(), request.getPassword());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, CookieUtil.createRefreshTokenCookie(tokens.getRefreshToken(), sslEnabled).toString())
                .body(new LoginResponse(tokens.getAccessToken(), tokens.getExpiresIn(), "Bearer"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        log.info("Refreshing token");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        TokensDto tokens = keycloakUserService.refreshTokens(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, CookieUtil.createRefreshTokenCookie(tokens.getRefreshToken(), sslEnabled).toString())
                .body(new LoginResponse(tokens.getAccessToken(), tokens.getExpiresIn(), "Bearer"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        log.info("Logout");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, CookieUtil.clearRefreshTokenCookie(sslEnabled).toString())
                .build();
    }
}
