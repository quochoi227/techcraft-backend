package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private Role role;
    private boolean emailVerified;
}
