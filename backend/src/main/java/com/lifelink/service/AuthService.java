package com.lifelink.service;

import com.lifelink.dto.AuthResponse;
import com.lifelink.dto.ForgotPasswordRequest;
import com.lifelink.dto.LoginRequest;
import com.lifelink.dto.MessageResponse;
import com.lifelink.dto.RegisterRequest;
import com.lifelink.dto.ResetPasswordRequest;
import com.lifelink.dto.UserProfileResponse;
import com.lifelink.entity.PasswordResetToken;
import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.Role;
import com.lifelink.exception.BadRequestException;
import com.lifelink.exception.DuplicateResourceException;
import com.lifelink.exception.ResourceNotFoundException;
import com.lifelink.repository.PasswordResetTokenRepository;
import com.lifelink.repository.UserRepository;
import com.lifelink.security.JwtService;
import com.lifelink.util.Haversine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final long RESET_TOKEN_VALIDITY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final String frontendUrl;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       EmailService emailService,
                       @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String phone = request.phone().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("An account with this phone number already exists");
        }

        Role role = parseRole(request.role());
        BloodGroup bloodGroup = null;
        if (request.bloodGroup() != null && !request.bloodGroup().isBlank()) {
            bloodGroup = BloodGroup.from(request.bloodGroup());
        } else if (role == Role.DONOR) {
            throw new BadRequestException("Blood group is required for donors");
        }
        validateCoordinates(request.latitude(), request.longitude());

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setBloodGroup(bloodGroup);
        user.setDistrict(trimToNull(request.district()));
        user.setAvailable(request.available());
        user.setLatitude(request.latitude());
        user.setLongitude(request.longitude());
        userRepository.save(user);

        emailService.sendWelcome(user.getEmail(), user);
        return buildAuthResponse(user, false);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Invalid email or password");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        return buildAuthResponse(user, request.remember());
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase()).ifPresent(user -> {
            String rawToken = generateRawToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(hashToken(rawToken));
            resetToken.setUser(user);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES));
            resetToken.setUsed(false);
            resetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password.html?token=" + rawToken;
            emailService.sendPasswordReset(user.getEmail(), user, resetLink);
        });
        // Always return success to avoid leaking which emails exist.
        return new MessageResponse("If that email exists, a reset link has been sent to it");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(hashToken(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("This reset link has already been used");
        }
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This reset link has expired. Please request a new one");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
        return new MessageResponse("Password updated successfully. You can now log in");
    }

    private AuthResponse buildAuthResponse(User user, boolean remember) {
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name(), remember);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(remember) / 1000, UserProfileResponse.from(user));
    }

    private Role parseRole(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Role is required");
        }
        try {
            Role role = Role.valueOf(raw.trim().toUpperCase());
            if (role == Role.ADMIN) {
                throw new BadRequestException("Admin accounts cannot be self-registered");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Role must be either DONOR or PATIENT");
        }
    }

    private static void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return;
        }
        if (!Haversine.isValidLatitude(latitude) || !Haversine.isValidLongitude(longitude)) {
            throw new BadRequestException("Location coordinates are out of range");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
