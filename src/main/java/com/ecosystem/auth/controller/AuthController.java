package com.ecosystem.auth.controller;


import com.ecosystem.auth.dto.LoginRequestDTO;
import com.ecosystem.auth.dto.LoginResponseDTO;
import com.ecosystem.auth.dto.ValidationResponseDTO;
import com.ecosystem.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/")
public class AuthController {

    @Autowired
    private AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest){

        Optional<LoginResponseDTO> authResult = authService.authenticate(loginRequest);
        return authResult.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        // send access token and refresh token
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidationResponseDTO> validate(@RequestHeader("Authorization") String authHeader){
        // Authorization: Bearer <token>
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<ValidationResponseDTO> validation = authService.validateToken(authHeader.substring(7));
        return validation.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }




}
