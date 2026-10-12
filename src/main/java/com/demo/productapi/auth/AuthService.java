package com.demo.productapi.auth;

import java.util.List;
import java.util.Locale;

import com.demo.productapi.config.SecurityConfig;
import com.demo.productapi.error.EmailAlreadyUsedException;
import com.demo.productapi.user.AppUser;
import com.demo.productapi.user.Role;
import com.demo.productapi.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()), Role.USER));
        return tokens.issue(user.getEmail(), List.of(user.getRole().name()));
    }

    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalize(request.email()), request.password()));
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(SecurityConfig.ROLE_PREFIX))
                .map(authority -> authority.substring(SecurityConfig.ROLE_PREFIX.length()))
                .toList();
        return tokens.issue(authentication.getName(), roles);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
