package com.ecosystem.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {

    private String accessToken;

    // рандомный набор символов, в бд хранится в хешированном виде
    private String refreshToken;
}
