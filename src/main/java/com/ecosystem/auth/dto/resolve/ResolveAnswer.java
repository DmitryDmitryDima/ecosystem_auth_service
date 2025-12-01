package com.ecosystem.auth.dto.resolve;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResolveAnswer {

    private boolean resolved;
    private UUID uuid;
}
