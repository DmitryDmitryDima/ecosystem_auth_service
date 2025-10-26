package com.ecosystem.auth.service;

import com.ecosystem.auth.dto.LoginRequestDTO;
import com.ecosystem.auth.dto.LoginResponseDTO;
import com.ecosystem.auth.dto.ValidationResponseDTO;
import com.ecosystem.auth.model.RefreshToken;
import com.ecosystem.auth.model.User;
import com.ecosystem.auth.repository.RefreshTokenRepository;
import com.ecosystem.auth.repository.UserRepository;
import com.ecosystem.auth.utils.JWTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTUtils jwtUtils;

    private String generateRandomString(){
        return UUID.randomUUID().toString();
    }

    private String generateAndSaveRefreshToken(User user){
        // генерируем, шифруем и сохраняем в бд access token
        String refreshToken = generateRandomString();
        RefreshToken refreshTokenEntity = new RefreshToken();
        Instant now = Instant.now();
        refreshTokenEntity.setToken(passwordEncoder.encode(refreshToken));
        refreshTokenEntity.setCreated_at(now);
        refreshTokenEntity.setExpired_at(now.plus(10, ChronoUnit.DAYS)); // 10 дней
        refreshTokenEntity.setUser(user);

        refreshTokenRepository.save(refreshTokenEntity);

        return refreshToken;
    }



    public Optional<LoginResponseDTO> authenticate(LoginRequestDTO loginRequest) {

        Optional<User> userBox = userRepository.findByUsername(loginRequest.getUsername());

        if (userBox.isEmpty()) return Optional.empty();


        User user = userBox.get();


        boolean passwordCheck = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

        if (!passwordCheck) return Optional.empty();

        // генерируем access токен на 10 часов
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());


        // генерируем, шифруем и сохраняем в бд access token
        String refreshToken = generateAndSaveRefreshToken(user);

        LoginResponseDTO response = new LoginResponseDTO(accessToken, refreshToken);

        return Optional.of(response);
    }

    // валидируем токен при каждом запросе в защищенную часть экосистемы
    public Optional<ValidationResponseDTO> validateToken(String token){

        return jwtUtils.validateToken(token);
    }
}
