package com.flightbooking.config;

import com.flightbooking.security.JwtAuthenticationFilter;
import com.flightbooking.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF - we use JWT which is not vulnerable to CSRF
            .csrf(AbstractHttpConfigurer::disable)

            // Define which endpoints are public and which require authentication
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints are public
                .requestMatchers("/api/auth/**").permitAll()

                // Flight search is public — anyone can search
                .requestMatchers(HttpMethod.GET, "/api/flights/**").permitAll()

                // Reference data (airports/airlines/aircraft) is public read-only lookup data
                .requestMatchers(HttpMethod.GET, "/api/airports/**", "/api/airlines/**", "/api/aircraft/**").permitAll()

                // Payment webhook is public (Razorpay calls this, not the browser)
                .requestMatchers("/api/payments/webhook").permitAll()

                // Admin endpoints require ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // Stateless session — we don't store sessions, JWT handles auth
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Security headers — defense-in-depth against common browser-based attacks.
            // These don't replace HTTPS (handled by the deployment platform) but add
            // extra protection on top of it.
            .headers(headers -> headers
                // Prevents the site from being embedded in an <iframe> on another
                // domain — stops clickjacking attacks where an attacker overlays
                // invisible buttons on top of your page.
                .frameOptions(frame -> frame.deny())

                // Stops browsers from trying to guess ("sniff") a different content
                // type than what the server declared — prevents certain XSS vectors
                // where a malicious file is served as if it were a script.
                .contentTypeOptions(contentTypeOptions -> {})

                // Tells browsers to only ever connect over HTTPS for the next year,
                // including subdomains. Once deployed behind HTTPS on Render, this
                // prevents any accidental fallback to plain HTTP.
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )

                // Controls how much of the URL is leaked in the Referer header when
                // navigating away from the site — avoids leaking booking references
                // or tokens embedded in query params to third-party sites.
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )

            // Our custom auth provider
            .authenticationProvider(authenticationProvider())

            // Rate limiting runs first — blocks brute-force attempts before they
            // even reach JWT validation or authentication logic.
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

            // Add JWT filter before Spring's username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 — strong enough for production
        return new BCryptPasswordEncoder(12);
    }
}
