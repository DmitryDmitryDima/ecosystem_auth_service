package com.ecosystem.auth.dto.logout;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleLogoutRequest {

    @NotBlank
    private String refreshToken;
}
