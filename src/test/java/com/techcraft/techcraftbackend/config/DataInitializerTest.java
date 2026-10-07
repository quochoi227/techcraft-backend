package com.techcraft.techcraftbackend.config;

import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataInitializer, "autoCreateAdmin", true);
        ReflectionTestUtils.setField(dataInitializer, "adminEmail", "admin@techcraft.com");
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "Admin@123456");
        ReflectionTestUtils.setField(dataInitializer, "adminFullName", "Administrator");
        ReflectionTestUtils.setField(dataInitializer, "adminPhone", "0123456789");
    }

    @Test
    @DisplayName("Should skip creation when admin already exists")
    void run_WhenAdminAlreadyExists_DoesNothing() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        dataInitializer.run();

        verify(userRepository, times(1)).existsByRole(Role.ADMIN);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should create default admin account when no admin exists and user does not exist")
    void run_WhenNoAdminAndUserDoesNotExist_CreatesAdmin() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@techcraft.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin@123456")).thenReturn("hashed-admin-password");

        dataInitializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser);
        assertEquals("admin@techcraft.com", savedUser.getEmail());
        assertEquals("hashed-admin-password", savedUser.getPasswordHash());
        assertEquals("Administrator", savedUser.getFullName());
        assertEquals("0123456789", savedUser.getPhone());
        assertEquals(Role.ADMIN, savedUser.getRole());
        assertTrue(savedUser.isEmailVerified());
    }

    @Test
    @DisplayName("Should promote existing user to admin when no admin exists but email matches")
    void run_WhenNoAdminAndUserExists_PromotesToAdmin() {
        User existingUser = User.builder()
                .email("admin@techcraft.com")
                .passwordHash("existing-hash")
                .fullName("Existing User")
                .role(Role.USER)
                .emailVerified(false)
                .build();

        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@techcraft.com")).thenReturn(Optional.of(existingUser));

        dataInitializer.run();

        assertEquals(Role.ADMIN, existingUser.getRole());
        assertTrue(existingUser.isEmailVerified());
        verify(userRepository, times(1)).save(existingUser);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should do nothing when auto-create is disabled")
    void run_WhenAutoCreateDisabled_DoesNothing() {
        ReflectionTestUtils.setField(dataInitializer, "autoCreateAdmin", false);

        dataInitializer.run();

        verify(userRepository, never()).existsByRole(any());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
