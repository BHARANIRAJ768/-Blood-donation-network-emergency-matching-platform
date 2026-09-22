package com.lifelink.service;

import com.lifelink.dto.AuthResponse;
import com.lifelink.dto.RegisterRequest;
import com.lifelink.dto.ResetPasswordRequest;
import com.lifelink.entity.PasswordResetToken;
import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.Role;
import com.lifelink.exception.BadRequestException;
import com.lifelink.exception.DuplicateResourceException;
import com.lifelink.repository.PasswordResetTokenRepository;
import com.lifelink.repository.UserRepository;
import com.lifelink.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordResetTokenRepository resetTokenRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    EmailService emailService;

    @InjectMocks
    AuthService authService;

    @Test
    void registerSavesUserAndReturnsToken() {
        when(userRepository.existsByEmailIgnoreCase("donor@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("1234567890")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(), anyString(), anyString(), eq(false))).thenReturn("jwt-token");
        when(jwtService.getExpirationMs(false)).thenReturn(86400000L);

        RegisterRequest payload = new RegisterRequest(
                "John Donor", " donor@test.com ", "1234567890", "password123",
                "DONOR", "A+", "Chennai", 13.0, 80.0, true);

        AuthResponse response = authService.register(payload);

        assertEquals("jwt-token", response.token());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("donor@test.com", saved.getEmail());
        assertEquals(Role.DONOR, saved.getRole());
        assertEquals(BloodGroup.A_POSITIVE, saved.getBloodGroup());
        assertEquals("hashed", saved.getPassword());
        verify(emailService).sendWelcome(eq("donor@test.com"), any(User.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("donor@test.com")).thenReturn(true);

        RegisterRequest payload = new RegisterRequest(
                "John Donor", "donor@test.com", "1234567890", "password123",
                "DONOR", "A+", "Chennai", null, null, true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(payload));
    }

    @Test
    void registerRejectsDonorWithoutBloodGroup() {
        when(userRepository.existsByEmailIgnoreCase("donor@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("1234567890")).thenReturn(false);

        RegisterRequest payload = new RegisterRequest(
                "John Donor", "donor@test.com", "1234567890", "password123",
                "DONOR", null, "Chennai", null, null, true);

        assertThrows(BadRequestException.class, () -> authService.register(payload));
    }

    @Test
    void registerRejectsAdminRole() {
        RegisterRequest payload = new RegisterRequest(
                "Admin", "admin2@test.com", "1234567890", "password123",
                "ADMIN", null, null, null, null, false);

        assertThrows(BadRequestException.class, () -> authService.register(payload));
    }

    @Test
    void resetPasswordInvalidatesToken() {
        String rawToken = "a".repeat(64);
        String hashed = "some-hash";
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(hashed);
        resetToken.setUsed(false);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        User user = new User();
        user.setId(1L);
        user.setPassword("old");
        resetToken.setUser(user);

        when(resetTokenRepository.findByToken(anyString())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");

        var message = authService.resetPassword(new ResetPasswordRequest(rawToken, "newPassword123"));

        assertEquals("Password updated successfully. You can now log in", message.message());
        assertEquals("new-hash", user.getPassword());
        assertTrue(resetToken.isUsed());
        verify(resetTokenRepository).save(resetToken);
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("hashed");
        resetToken.setUsed(false);
        resetToken.setExpiryDate(LocalDateTime.now().minusMinutes(5));
        resetToken.setUser(new User());

        when(resetTokenRepository.findByToken(anyString())).thenReturn(Optional.of(resetToken));

        assertThrows(BadRequestException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("token", "newPassword123")));
    }
}
