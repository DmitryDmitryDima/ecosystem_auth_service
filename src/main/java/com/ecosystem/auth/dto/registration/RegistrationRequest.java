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
    @Size(min = 4, max = 30, message = "we will accept username with length between 4 and 30 symbols")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 50, message = "we will accept password with length between 8 and 50 symbols")
    private String password;


}
