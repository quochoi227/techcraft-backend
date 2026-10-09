package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.RegisterResponse;
import com.techcraft.techcraftbackend.dto.response.UserSummaryResponse;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void toRegisterResponse_Success() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("test@techcraft.com")
                .fullName("Nguyễn Văn A")
                .phone("0987654321")
                .address("123 Đường ABC, Hà Nội")
                .role(Role.USER)
                .emailVerified(false)
                .build();

        RegisterResponse response = userMapper.toRegisterResponse(user);

        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("test@techcraft.com", response.getEmail());
        assertEquals("Nguyễn Văn A", response.getFullName());
        assertEquals("0987654321", response.getPhone());
        assertEquals("123 Đường ABC, Hà Nội", response.getAddress());
        assertEquals(Role.USER, response.getRole());
        assertFalse(response.isEmailVerified());
    }

    @Test
    void toRegisterResponse_NullInput_ReturnsNull() {
        assertNull(userMapper.toRegisterResponse(null));
    }

    @Test
    void toUserSummaryResponse_Success() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("admin@techcraft.com")
                .fullName("Trần Thị Admin")
                .phone("0912345678")
                .address("456 Đường XYZ, TP.HCM")
                .role(Role.ADMIN)
                .emailVerified(true)
                .build();

        UserSummaryResponse response = userMapper.toUserSummaryResponse(user);

        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("admin@techcraft.com", response.getEmail());
        assertEquals("Trần Thị Admin", response.getFullName());
        assertEquals("0912345678", response.getPhone());
        assertEquals("456 Đường XYZ, TP.HCM", response.getAddress());
        assertEquals(Role.ADMIN, response.getRole());
        assertTrue(response.isEmailVerified());
    }

    @Test
    void toUserSummaryResponse_NullInput_ReturnsNull() {
        assertNull(userMapper.toUserSummaryResponse(null));
    }
}
