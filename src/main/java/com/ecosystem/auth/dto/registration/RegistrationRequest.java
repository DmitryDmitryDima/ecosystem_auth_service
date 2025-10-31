package com.ecosystem.auth.dto.registration;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "username is required!")
    @Size(min = 4, max = 30, message = "Нужна длина от 4 до 30 символов")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 50, message = "Нужна длина от 8 до 50 символов")
    private String password;


}
