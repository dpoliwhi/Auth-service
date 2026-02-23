package ru.dpoliwhi.authservice.util;

import org.springframework.http.ResponseCookie;

public class CookieUtil {

    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;

    public static ResponseCookie createRefreshTokenCookie(String refreshToken, boolean secure) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/user/refresh")
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .build();
    }

    public static ResponseCookie clearRefreshTokenCookie(boolean secure) {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/user/refresh")
                .maxAge(0)
                .build();
    }
}
