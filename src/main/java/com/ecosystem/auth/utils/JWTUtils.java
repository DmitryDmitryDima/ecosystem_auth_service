package com.ecosystem.auth.utils;


import com.ecosystem.auth.dto.login.LoginResponseDTO;
import com.ecosystem.auth.dto.utils.AccessTokenInfo;
import com.ecosystem.auth.dto.validation.ValidationResponseDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JWTUtils {


    private final Key secretKey;

    public JWTUtils(@Value("${jwt.secret}") String secret){
        byte[] keyBytes = Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8));
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }



    // создаем токен, вкладывая информацию о uuid, username, role (эта инфа будет передана вглубь микросервисов)
    public AccessTokenInfo generateToken(UUID uuid, String username, String role){
        Date expirationTime = new Date(System.currentTimeMillis() + 1000 * 60 * 15);

        String accessToken = Jwts.builder()
                .subject(uuid.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(expirationTime)  // 5 часов
                .signWith(secretKey)
                .compact();
        AccessTokenInfo accessTokenInfo = new AccessTokenInfo(accessToken, expirationTime.getTime());

        return accessTokenInfo;
    }


    public Optional<ValidationResponseDTO> validateToken(String token){
        try {
            Jws<Claims> claims = Jwts.parser().verifyWith((SecretKey) secretKey)
                    .build()
                    .parseSignedClaims(token);
            String uuid = claims.getPayload().getSubject();
            String username = claims.getPayload().get("username", String.class);
            String role = claims.getPayload().get("role", String.class);

            return Optional.of(new ValidationResponseDTO(UUID.fromString(uuid), username, role));
        }
        catch (JwtException validationException){
            return Optional.empty();
        }

    }





}
