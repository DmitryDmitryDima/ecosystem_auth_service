package com.ecosystem.auth.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCreationEvent {


    // часть контекста, дублируемого в user service

    private UUID uuid;
    private String role;
    private String username;
}
