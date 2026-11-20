package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private Role role;
    private boolean emailVerified;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
