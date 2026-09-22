package com.lifelink.config;

import com.lifelink.entity.User;
import com.lifelink.enums.Role;
import com.lifelink.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the administrator account on first start.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
    }

    private void seedAdmin() {
        String email = "admin@lifelink.com";
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User admin = new User();
        admin.setName("VitalDrop Administrator");
        admin.setEmail(email);
        admin.setPhone("+10000000000");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setAvailable(false);
        userRepository.save(admin);
        log.info("Seeded administrator account: {} / Admin@123", email);
    }
}
