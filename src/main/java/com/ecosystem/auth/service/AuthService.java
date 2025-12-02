package com.ecosystem.auth.service;

import com.ecosystem.auth.dto.events.UserCreationEvent;
import com.ecosystem.auth.dto.login.LoginRequestDTO;
import com.ecosystem.auth.dto.login.LoginResponseDTO;
import com.ecosystem.auth.dto.logout.SimpleLogoutRequest;
import com.ecosystem.auth.dto.refresh.RefreshRequest;
import com.ecosystem.auth.dto.refresh.RefreshResponse;
import com.ecosystem.auth.dto.registration.RegistrationAnswer;
import com.ecosystem.auth.dto.registration.RegistrationRequest;

import com.ecosystem.auth.dto.utils.AccessTokenInfo;
import com.ecosystem.auth.dto.validation.ValidationResponseDTO;
import com.ecosystem.auth.model.RefreshToken;
import com.ecosystem.auth.model.User;
import com.ecosystem.auth.repository.RefreshTokenRepository;
import com.ecosystem.auth.repository.UserRepository;
import com.ecosystem.auth.utils.JWTUtils;
import jakarta.transaction.Transactional;
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

    @Autowired
    private RabbitProducerService rabbitProducerService;

    private String generateRandomString(){
        return UUID.randomUUID().toString();
    }

    // хешируем refresh токен для бд
    // todo - хеширование refresh токена должно содержать в себе api secret
    private String hashRefreshToken(String refreshToken) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedHash);
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
        // генерируем raw refresh токен
        String refreshToken = generateRandomString();

        // хешируем refresh токен для бд
        String refreshTokenEncoded = hashRefreshToken(refreshToken);


        // создаем сущность refresh token, сохраняем ее в бд
        RefreshToken refreshTokenEntity = new RefreshToken();
        Instant now = Instant.now();
        refreshTokenEntity.setToken(refreshTokenEncoded);
        refreshTokenEntity.setCreated_at(now);
        refreshTokenEntity.setExpired_at(now.plus(10, ChronoUnit.DAYS)); // 10 дней
        refreshTokenEntity.setUser(user);
        refreshTokenRepository.save(refreshTokenEntity);

        return refreshToken;
    }




    // аутентификация при вводе логина и пароля
    public Optional<LoginResponseDTO> authenticate(LoginRequestDTO loginRequest) {

        // проверка - существует ли юзер с присланным username (username - уникален)
        Optional<User> userBox = userRepository.findByUsername(loginRequest.getUsername());
        if (userBox.isEmpty()) return Optional.empty();

        // извлекаем сущность юзера
        User user = userBox.get();

        // проверка пароля на соответствие введенному пользователем (специальная функция password encoder)
        boolean passwordCheck = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

        if (!passwordCheck) return Optional.empty();

        // генерируем access токен на 15 минут
        AccessTokenInfo accessTokenResponse = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());


        // генерируем, шифруем и сохраняем в бд refresh token
        String refreshToken;

        try {
            refreshToken = generateAndSaveRefreshToken(user);
        }
        catch (Exception e){
            return Optional.empty();
        }

        // собираем ответ для юзера
        return Optional.of(new LoginResponseDTO(accessTokenResponse.getAccessToken(),
                refreshToken));
    }



    // валидируем токен при каждом запросе в защищенную часть экосистемы
    public Optional<ValidationResponseDTO> validateToken(String token){

        return jwtUtils.validateToken(token);
    }
    // выясняем uuid для username
    public Optional<UUID> resolve(String username){
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(User::getId);
    }





    // обновляем access и refresh токены с помощью refresh токена
    @Transactional
    public Optional<RefreshResponse> refresh(RefreshRequest request){


        // хешируем refresh токен для последующего его сравнения с бд
        String encodedToken;
        try {
            encodedToken = hashRefreshToken(request.getRefreshToken());
        }
        catch (Exception e){
            return Optional.empty();
        }

        // проверяем, есть ли в базе токен присланный пользователем
        Optional<RefreshToken> tokenCheck = refreshTokenRepository.findByToken(encodedToken);

        if (tokenCheck.isEmpty()) return Optional.empty();

        RefreshToken tokenEntity = tokenCheck.get();

        // проверка на бан
        if (tokenEntity.isRevoked()){
            return Optional.empty();
        }

        // проверка на просрочку (помечаем токен, как revoked, после чего он удаляется специальным фоновым процессом)
        if (Instant.now().isAfter(tokenEntity.getExpired_at())) {
            tokenEntity.setRevoked(true);
            return Optional.empty();
        }


        // извлекаем связанного с токеном пользователя (manyToOne ассоциация между refresh_token & user)
        User user = tokenEntity.getUser();

        // генерируем новую пару access token & refresh token
        AccessTokenInfo accessTokenInfo = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        String refreshToken;
        try {
            refreshToken = generateAndSaveRefreshToken(user);
        } catch (NoSuchAlgorithmException e) {


            return Optional.empty();
        }

        return Optional.of(new RefreshResponse(accessTokenInfo.getAccessToken(),
                refreshToken));


    }

    // регистрируем пользователя, проверяем, существует ли username
    @Transactional
    public RegistrationAnswer registration(RegistrationRequest request) throws Exception {
        String username = request.getUsername();
        Optional<User> userCheck = userRepository.findByUsername(username);
        if (userCheck.isPresent()) return new RegistrationAnswer("Имя занято", false);

        User newUser = new User();
        newUser.setRole("USER");
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setUsername(username);

        userRepository.save(newUser);

        // публикуем событие - в случае исключения откат транзакции
        rabbitProducerService.generateUserCreationEvent(UserCreationEvent.builder()
                .uuid(newUser.getId()).build());



        return new RegistrationAnswer("Регистрация завершена", true);


    }

    // отзыв токена
    @Transactional
    public void simpleLogout(SimpleLogoutRequest request){
        try {
            String encodedToken = hashRefreshToken(request.getRefreshToken());
            RefreshToken token = refreshTokenRepository.findByToken(encodedToken).orElseThrow();
            token.setRevoked(true);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
