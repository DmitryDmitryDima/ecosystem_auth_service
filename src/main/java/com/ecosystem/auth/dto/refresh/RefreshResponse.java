package com.ecosystem.auth.dto.refresh;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshResponse {

    private String accessToken;

    // новый рефреш токен
    private String refreshToken;


}
