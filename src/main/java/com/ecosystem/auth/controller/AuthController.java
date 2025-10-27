package com.ecosystem.auth.controller;


import com.ecosystem.auth.dto.login.LoginRequestDTO;
import com.ecosystem.auth.dto.login.LoginResponseDTO;
import com.ecosystem.auth.dto.logout.SimpleLogoutRequest;
import com.ecosystem.auth.dto.refresh.RefreshRequest;
import com.ecosystem.auth.dto.refresh.RefreshResponse;
import com.ecosystem.auth.dto.registration.RegistrationAnswer;
import com.ecosystem.auth.dto.registration.RegistrationRequest;
import com.ecosystem.auth.dto.validation.ValidationResponseDTO;
import com.ecosystem.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


/*
микросервис доступен по адресу /auth/... в api gateway, открыт для всех запросов
 */

@RestController
@RequestMapping("/")
public class AuthController {

    @Autowired
    private AuthService authService;


    // аутентификация существуюго пользователя
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest){

        Optional<LoginResponseDTO> authResult = authService.authenticate(loginRequest);
        return authResult
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());


    }

    // валидируем пользователя через access token
    @GetMapping("/validate")
    public ResponseEntity<ValidationResponseDTO> validate(@RequestHeader("Authorization") String authHeader){
        // Authorization: Bearer <token>
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<ValidationResponseDTO> validation = authService.validateToken(authHeader.substring(7));
        return validation.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }


    // обновляем access токен через refresh токен
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest request){

        Optional<RefreshResponse> refreshResult = authService.refresh(request);
        return refreshResult
                .map(ResponseEntity::ok)
                .orElseGet(()->ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // регистрация нового пользователя
    @PostMapping("/register")
    public ResponseEntity<RegistrationAnswer> register(@RequestBody RegistrationRequest request){

        RegistrationAnswer answer = authService.registration(request);

        return answer.isSuccess()?
                ResponseEntity.ok(answer)
                :
                ResponseEntity.status(HttpStatus.CONFLICT).body(answer);

    }

    // logout - пока что заключается в том, что мы делаем revoke для refresh токена, если он существует
    // отдельный челлендж - logout на всех устройствах
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody SimpleLogoutRequest logoutRequest){

        authService.simpleLogout(logoutRequest);

        return ResponseEntity.noContent().build();
    }








}
