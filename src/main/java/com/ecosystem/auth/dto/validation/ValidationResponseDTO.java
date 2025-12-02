package com.ecosystem.auth.dto.validation;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidationResponseDTO {

    private UUID uuid;

    private String username;

    private String role;

    private UUID targetUUID;

}
