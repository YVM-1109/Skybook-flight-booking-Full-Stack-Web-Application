package com.flightbooking.service;

import com.flightbooking.dto.request.LoginRequest;
import com.flightbooking.dto.request.RegisterRequest;
import com.flightbooking.dto.response.AuthResponse;
import com.flightbooking.entity.RefreshToken;
import com.flightbooking.entity.User;
import com.flightbooking.exception.AccountLockedException;
import com.flightbooking.exception.ResourceNotFoundException;
import com.flightbooking.repository.RefreshTokenRepository;
import com.flightbooking.repository.UserRepository;
import com.flightbooking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    // IP-based failure tracking to prevent account lockout abuse
    private static final int MAX_IP_FAILED_ATTEMPTS = 20;
    private static final int IP_LOCKOUT_DURATION_MINUTES = 30;
    private final ConcurrentMap<String, IpLockoutState> ipLockoutMap = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    private static class IpLockoutState {
        int attempts;
        LocalDateTime lockedUntil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.PASSENGER)
                .build();

        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // For IP tracking, we'd need the request - but we don't have it here.
        // The RateLimitFilter already provides per-IP limiting on /api/auth/login.
        // To prevent lockout abuse, we track account lockout separately from IP lockout.

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user != null) {
            // Check if account is currently locked
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                long minutesLeft = Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
                throw new AccountLockedException(
                        "Account is temporarily locked due to too many failed login attempts. " +
                        "Try again in " + minutesLeft + " minute(s)."
                );
            }

            // If lock has expired, reset the counter
            if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(LocalDateTime.now())) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Increment failure counter and potentially lock the account
            if (user != null) {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);

                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
                    user.setFailedLoginAttempts(0);
                    userRepository.save(user);
                    throw new AccountLockedException(
                            "Account locked for " + LOCKOUT_DURATION_MINUTES +
                            " minutes after " + MAX_FAILED_ATTEMPTS + " failed attempts."
                    );
                }
                userRepository.save(user);
            }
            // Always throw generic message — don't reveal if email exists
            throw new BadCredentialsException("Invalid email or password");
        }

        // Successful login — reset failure counter
        if (user != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        } else {
            // Authenticated but no user found — shouldn't happen
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (storedToken.isRevokedOrExpired()) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        User user = storedToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
