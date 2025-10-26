package com.ecosystem.auth.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
валидированный запрос к бд
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "username is required!")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 50, message = "we will accept password with length between 8 and 50 symbols")
    private String password;


}
