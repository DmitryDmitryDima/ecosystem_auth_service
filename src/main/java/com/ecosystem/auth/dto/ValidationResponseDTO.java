package com.ecosystem.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResponseDTO {

    private UUID uuid;

    private String username;

    private String role;

}
