package com.ecosystem.auth.controller;


import com.ecosystem.auth.dto.login.LoginRequestDTO;
import com.ecosystem.auth.dto.login.LoginResponseDTO;
import com.ecosystem.auth.dto.logout.SimpleLogoutRequest;
import com.ecosystem.auth.dto.refresh.RefreshRequest;
import com.ecosystem.auth.dto.refresh.RefreshResponse;
import com.ecosystem.auth.dto.registration.RegistrationAnswer;
import com.ecosystem.auth.dto.registration.RegistrationRequest;

import com.ecosystem.auth.dto.resolve.UsernameUUIDPair;
import com.ecosystem.auth.dto.validation.ValidationResponseDTO;
import com.ecosystem.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/*
микросервис доступен по адресу /auth/... в api gateway, открыт для всех запросов
 */

@RestController
@RequestMapping("/")
public class AuthController {

    @Autowired
    private AuthService authService;




    @GetMapping("/identify")
    public ResponseEntity<UsernameUUIDPair> identify(@CookieValue(value = "accessToken", required = false) String accessToken){
        // для гостя - пустые поля
        if (accessToken==null) return ResponseEntity.ok(new UsernameUUIDPair());

        Optional<ValidationResponseDTO> validationCheck = authService.validateToken(accessToken);
        // если пользователь, имея токен, получает ошибку, то отвечаем с 401
        return validationCheck.map(validationResponseDTO -> ResponseEntity.ok(UsernameUUIDPair.builder()
                .uuid(validationResponseDTO.getUuid())
                .username(validationResponseDTO.getUsername())
                .build()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());


    }















    // аутентификация существующего пользователя с помощью username и password
    // возвращаем пару access token + refresh токен с времени просрочки access токена
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequestDTO loginRequest, HttpServletResponse response){

        System.out.println("login request");



        Optional<LoginResponseDTO> authResult = authService.authenticate(loginRequest);
        if (authResult.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();


        Cookie accessCookie = new Cookie("accessToken", authResult.get().getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(7 * 24 * 60 * 60);

        Cookie refreshCookie = new Cookie("refreshToken", authResult.get().getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);



        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.noContent().build();









    }




    @GetMapping("/search")
    public ResponseEntity<Page<UsernameUUIDPair>> search(Pageable pageable, @RequestParam("startsWith") String startsWith){
        System.out.println(pageable.getPageSize());
        System.out.println(pageable.getPageNumber());
        System.out.println(pageable);
        return ResponseEntity.ok(authService.searchUsers(pageable, startsWith));
    }




    // todo need caching
    @GetMapping("/resolveUUID/{uuid}")
    public ResponseEntity<UsernameUUIDPair> resolveUUID(@PathVariable("uuid") UUID uuid){

        Optional<UsernameUUIDPair> resolveDTOCheck = authService.resolve(uuid);
        return resolveDTOCheck.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());


    }

    @GetMapping("/resolveBatch")
    public ResponseEntity<List<UsernameUUIDPair>> batchedResolveUsername(@RequestParam String uuids){
        try {
            List<UUID> parsedUUIDs = Arrays.stream(uuids.split(",")).map(UUID::fromString).toList();
            return ResponseEntity.ok(authService.resolve(parsedUUIDs));
        }
        catch (Exception e){
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
//
    @GetMapping("/resolveUsername/{username}")
    public ResponseEntity<UUID> resolveUsername(@PathVariable("username") String username){
        Optional<UUID> uuid = authService.resolve(username);
        return uuid.map(ResponseEntity::ok).orElseGet(()->
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        );
    }


    // валидируем пользователя через access token (endpoint вызывается из gateway фильтра)
    // возвращаем uuid, username, role  - security context, или 401 в случае ошибки

    @GetMapping("/validate")
    public ResponseEntity<ValidationResponseDTO> validate(
                                                          @CookieValue(value = "accessToken", required = false) String accessToken,
                                                          @RequestHeader(value = "targetUsername", required = false) String targetUsername){

        System.out.println("validate request");

        UUID target = targetUsername==null?null:authService.resolve(targetUsername).get();




        // гость
        if(accessToken==null) {



            return ResponseEntity.ok(ValidationResponseDTO.builder()
                            .role("GUEST")
                            .targetUUID(target)
                    .build());

        }



        Optional<ValidationResponseDTO> validationCheck = authService.validateToken(accessToken);
        // если пользователь, имея токен, получает ошибку, то отвечаем с 401
        if (validationCheck.isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // дополняем dto target uuid, если оно присутствует
        ValidationResponseDTO validationResponseDTO = validationCheck.get();
        validationResponseDTO.setTargetUUID(target);
        return ResponseEntity.ok(validationResponseDTO);

    }


    // обновляем access токен через refresh токен
    // возвращаем новую пару access токен + refresh токен
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                        HttpServletResponse response){

        System.out.println("refresh request");
        if (refreshToken == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<RefreshResponse> refreshResult = authService.refresh(refreshToken);
        if (refreshResult.isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        else {
            Cookie accessCookie = new Cookie("accessToken", refreshResult.get().getAccessToken());
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(7 * 24 * 60 * 60);

            Cookie refreshCookie = new Cookie("refreshToken", refreshResult.get().getRefreshToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60);
            response.addCookie(accessCookie);
            response.addCookie(refreshCookie);
        }
        return ResponseEntity.noContent().build(); // 204
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
    @PostMapping("/revoke")
    public ResponseEntity<Void> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                       HttpServletResponse response){

        System.out.println("logout");

        if (refreshToken!=null){
            authService.simpleLogout(refreshToken);
        }



        Cookie accessCookie = new Cookie("accessToken",null);
        accessCookie.setMaxAge(0); // мгновенное удаление из браузера
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");


        Cookie refreshCookie = new Cookie("refreshToken",null);
        refreshCookie.setMaxAge(0); // мгновенное удаление из браузера
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.noContent().build();
    }








}
