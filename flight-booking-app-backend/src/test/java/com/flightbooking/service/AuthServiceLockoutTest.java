package com.flightbooking.service;

import com.flightbooking.dto.request.LoginRequest;
import com.flightbooking.entity.User;
import com.flightbooking.exception.AccountLockedException;
import com.flightbooking.repository.RefreshTokenRepository;
import com.flightbooking.repository.UserRepository;
import com.flightbooking.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Covers the account lockout behavior added to AuthService: an account
// locks after 5 consecutive failed logins, stays locked for 15 minutes,
// and resets its failure counter on the next successful login or once
// the lock window has passed.
@ExtendWith(MockitoExtension.class)
class AuthServiceLockoutTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks
    AuthService authService;

    private User user;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("yash@test.com")
                .passwordHash("hashed")
                .fullName("Yash")
                .role(User.Role.PASSENGER)
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("yash@test.com");
        loginRequest.setPassword("wrong-password");
    }

    @Test
    void login_incrementsFailedAttempts_onBadCredentials() {
        when(userRepository.findByEmail("yash@test.com")).thenReturn(Optional.of(user));
        doThrow(new BadCredentialsException("bad")).when(authenticationManager)
                .authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

        assertEquals(1, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
        verify(userRepository, atLeastOnce()).save(user);
    }

    @Test
    void login_locksAccount_afterFiveFailedAttempts() {
        user.setFailedLoginAttempts(4); // one more failure should trip the lock
        when(userRepository.findByEmail("yash@test.com")).thenReturn(Optional.of(user));
        doThrow(new BadCredentialsException("bad")).when(authenticationManager)
                .authenticate(any());

        assertThrows(AccountLockedException.class, () -> authService.login(loginRequest));

        assertEquals(0, user.getFailedLoginAttempts()); // counter resets once locked
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now()));
    }

    @Test
    void login_rejectsImmediately_whileAccountIsLocked() {
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("yash@test.com")).thenReturn(Optional.of(user));

        assertThrows(AccountLockedException.class, () -> authService.login(loginRequest));

        // Authentication should never even be attempted while locked
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void login_allowsAttempt_onceLockWindowHasPassed() {
        // Lock expired 1 minute ago — should be treated as unlocked
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        user.setFailedLoginAttempts(5);
        when(userRepository.findByEmail("yash@test.com")).thenReturn(Optional.of(user));
        // Authentication succeeds this time
        loginRequest.setPassword("correct-password");
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        authService.login(loginRequest);

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    void login_resetsFailedAttempts_onSuccessfulLogin() {
        user.setFailedLoginAttempts(3);
        when(userRepository.findByEmail("yash@test.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        authService.login(loginRequest);

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    void login_doesNotRevealWhetherEmailExists_onUnknownUser() {
        loginRequest.setEmail("nobody@test.com");
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
        doThrow(new BadCredentialsException("bad")).when(authenticationManager)
                .authenticate(any());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", ex.getMessage());
        // No user row to update since none exists
        verify(userRepository, never()).save(any());
    }
}
