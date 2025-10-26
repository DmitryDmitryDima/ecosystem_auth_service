package com.ecosystem.auth.service;

import com.ecosystem.auth.dto.login.LoginRequestDTO;
import com.ecosystem.auth.dto.login.LoginResponseDTO;
import com.ecosystem.auth.dto.refresh.RefreshRequest;
import com.ecosystem.auth.dto.refresh.RefreshResponse;
import com.ecosystem.auth.dto.utils.AccessTokenInfo;
import com.ecosystem.auth.dto.validation.ValidationResponseDTO;
import com.ecosystem.auth.model.RefreshToken;
import com.ecosystem.auth.model.User;
import com.ecosystem.auth.repository.RefreshTokenRepository;
import com.ecosystem.auth.repository.UserRepository;
import com.ecosystem.auth.utils.JWTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    // хешируем refresh токен для бд
    private String hashRefreshToken(String refreshToken) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedhash);
    }

    // Пошаговое преобразование байтов в шестнадцатеричную строку
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


    // генерируем refresh токен
    private String generateAndSaveRefreshToken(User user) throws NoSuchAlgorithmException {
        // генерируем, шифруем и сохраняем в бд access token
        String refreshToken = generateRandomString();

        String refreshTokenEncoded = hashRefreshToken(refreshToken);



        RefreshToken refreshTokenEntity = new RefreshToken();
        Instant now = Instant.now();
        refreshTokenEntity.setToken(refreshTokenEncoded);
        refreshTokenEntity.setCreated_at(now);
        refreshTokenEntity.setExpired_at(now.plus(10, ChronoUnit.DAYS)); // 10 дней
        refreshTokenEntity.setUser(user);

        refreshTokenRepository.save(refreshTokenEntity);

        return refreshToken;
    }


    // аутентификация при вводу логина и пароля
    public Optional<LoginResponseDTO> authenticate(LoginRequestDTO loginRequest) {

        Optional<User> userBox = userRepository.findByUsername(loginRequest.getUsername());

        if (userBox.isEmpty()) return Optional.empty();


        User user = userBox.get();


        boolean passwordCheck = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

        if (!passwordCheck) return Optional.empty();

        // генерируем access токен на 10 часов
        AccessTokenInfo accessTokenResponse = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());



        String refreshToken;
        // генерируем, шифруем и сохраняем в бд access token
        try {
            refreshToken = generateAndSaveRefreshToken(user);
        }
        catch (Exception e){
            return Optional.empty();
        }





        return Optional.of(new LoginResponseDTO(accessTokenResponse.getAccessToken(),
                refreshToken, accessTokenResponse.getExpired_at()));
    }



    // валидируем токен при каждом запросе в защищенную часть экосистемы
    public Optional<ValidationResponseDTO> validateToken(String token){

        return jwtUtils.validateToken(token);
    }





    // обновляем access и refresh токены с помощью refresh токена

    // todo проверка на просрочку и отзыв
    public Optional<RefreshResponse> refresh(RefreshRequest request){


        String encodedToken;
        try {
            encodedToken = hashRefreshToken(request.getRefreshToken());
        }
        catch (Exception e){
            return Optional.empty();
        }


        Optional<RefreshToken> tokenCheck = refreshTokenRepository.findByToken(encodedToken);

        if (tokenCheck.isEmpty()) return Optional.empty();

        User user = tokenCheck.get().getUser();

        AccessTokenInfo accessTokenInfo = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        String refreshToken;
        try {
            refreshToken = generateAndSaveRefreshToken(user);
        } catch (NoSuchAlgorithmException e) {


            return Optional.empty();
        }

        return Optional.of(new RefreshResponse(accessTokenInfo.getAccessToken(),
                refreshToken,
                accessTokenInfo.getExpired_at()));




    }
}
