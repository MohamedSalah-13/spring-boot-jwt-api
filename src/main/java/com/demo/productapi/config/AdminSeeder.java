package com.demo.productapi.config;

import java.util.Locale;

import com.demo.productapi.user.AppUser;
import com.demo.productapi.user.Role;
import com.demo.productapi.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Creates the initial ADMIN account from configuration, so no credentials live in the code base. */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AdminProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AdminProperties properties, UserRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            return;
        }
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            return;
        }
        users.save(new AppUser(email, passwordEncoder.encode(properties.password()), Role.ADMIN));
        log.info("Created admin account {}", email);
    }
}
