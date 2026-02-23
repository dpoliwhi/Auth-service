package ru.dpoliwhi.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokensDto {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
}
