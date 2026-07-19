package com.ht_rnd.wifi_admin_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures HTTP security and in-memory authentication for the application.
 * This configuration protects REST endpoints and actuator routes using
 * HTTP Basic authentication. Credentials are loaded from application
 * properties and stored in an in-memory user details manager.
 */
@Configuration
public class SecurityConfig {

    /**
     * Username used for in-memory authentication.
     */
    @Value("${app.security.username}")
    private String username;

    /**
     * Password used for in-memory authentication.
     */
    @Value("${app.security.password}")
    private String password;

    /**
     * Configures the application's HTTP security rules.
     * Health and info actuator endpoints are publicly accessible, actuator
     * management endpoints require the {@code ADMIN} role, and Wi-Fi parameter
     * endpoints require authentication.
     *
     * @param http Spring Security HTTP configuration builder
     * @return configured security filter chain
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/wifi-parameter/**").authenticated()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Creates the in-memory user store used for authentication.
     * A single administrative user is created from credentials defined in
     * the application configuration.
     *
     * @param passwordEncoder password encoder used to hash the configured password
     * @return in-memory user details manager containing the configured user
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * Creates the password encoder used for authentication.
     * The delegating password encoder supports multiple encoding formats and
     * uses a secure default for newly encoded passwords.
     *
     * @return configured password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}