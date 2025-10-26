package com.ecosystem.auth.dto.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessTokenInfo {


    private String accessToken;
    private Long expired_at;
}
