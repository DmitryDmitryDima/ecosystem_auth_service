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




    // аутентификация существующего пользователя с помощью username и password
    // возвращаем пару access token + refresh токен с времени просрочки access токена
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest){

        Optional<LoginResponseDTO> authResult = authService.authenticate(loginRequest);

        return authResult
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());


    }

    // валидируем пользователя через access token (endpoint вызывается из gateway фильтра)
    // возвращаем uuid, username, role  - security context, или 401 в случае ошибки
    @GetMapping("/validate")
    public ResponseEntity<ValidationResponseDTO> validate(@RequestHeader("Authorization") String authHeader){
        // Authorization: Bearer <token>
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        System.out.println("validation here for "+authHeader);
        Optional<ValidationResponseDTO> validation = authService.validateToken(authHeader.substring(7));
        return validation.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }


    // обновляем access токен через refresh токен
    // возвращаем новую пару access токен + refresh токен
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest request){

        System.out.println("refresh token fetched");

        Optional<RefreshResponse> refreshResult = authService.refresh(request);
        return refreshResult
                .map(ResponseEntity::ok)
                .orElseGet(()->ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // регистрация нового пользователя
    // возвращаем сообщение (в случае ошибки оно будет объяснять, что пользователь сделал не так)
    @PostMapping("/register")
    public ResponseEntity<RegistrationAnswer> register(@RequestBody RegistrationRequest request){


        try {
            RegistrationAnswer registrationAnswer = authService.registration(request);
            return registrationAnswer.isSuccess()?
                    ResponseEntity.ok(registrationAnswer)
                    :
                    ResponseEntity.status(HttpStatus.CONFLICT).body(registrationAnswer);

        }
        catch (Exception e){
            RegistrationAnswer registrationAnswer = new RegistrationAnswer();
            registrationAnswer.setSuccess(false);
            registrationAnswer.setMessage("Внутренняя ошибка сервера, попробуйте позже");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(registrationAnswer);

        }









    }

    // logout - пока что заключается в том, что мы делаем revoke для refresh токена, если он существует
    // будущий челлендж - logout на всех устройствах
    @PostMapping("/revoke")
    public ResponseEntity<Void> logout(@RequestBody SimpleLogoutRequest logoutRequest){

        System.out.println("logout");

        authService.simpleLogout(logoutRequest);

        return ResponseEntity.noContent().build();
    }








}
