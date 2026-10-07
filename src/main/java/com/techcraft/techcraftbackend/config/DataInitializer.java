package com.techcraft.techcraftbackend.config;

import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.auto-create:true}")
    private boolean autoCreateAdmin;

    @Value("${app.admin.email:admin@techcraft.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123456}")
    private String adminPassword;

    @Value("${app.admin.full-name:Administrator}")
    private String adminFullName;

    @Value("${app.admin.phone:0123456789}")
    private String adminPhone;

    @Override
    @Transactional
    public void run(String... args) {
        if (!autoCreateAdmin) {
            log.info("Admin auto-creation is disabled by configuration (app.admin.auto-create=false).");
            return;
        }

        String normalizedEmail = adminEmail.toLowerCase().trim();

        // Kiểm tra xem đã có tài khoản Admin nào tồn tại trong hệ thống chưa
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin account already exists in database. Skipping admin initialization.");
            return;
        }

        // Nếu chưa có tài khoản Admin nào, tiến hành tạo hoặc nâng cấp tài khoản
        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(
                existingUser -> {
                    log.info("User with email '{}' already exists without ADMIN role. Promoting to ADMIN.", normalizedEmail);
                    existingUser.setRole(Role.ADMIN);
                    existingUser.setEmailVerified(true);
                    userRepository.save(existingUser);
                    log.info("Successfully promoted user '{}' to ADMIN role.", normalizedEmail);
                },
                () -> {
                    log.info("No ADMIN found. Creating default admin account with email: {}", normalizedEmail);
                    User admin = User.builder()
                            .email(normalizedEmail)
                            .passwordHash(passwordEncoder.encode(adminPassword))
                            .fullName(adminFullName != null ? adminFullName.trim() : "Administrator")
                            .phone(adminPhone != null ? adminPhone.trim() : null)
                            .role(Role.ADMIN)
                            .emailVerified(true)
                            .build();

                    userRepository.save(admin);
                    log.info("Default admin account created successfully! Email: {}", normalizedEmail);
                }
        );
    }
}
