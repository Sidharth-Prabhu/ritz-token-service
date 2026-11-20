package com.rit.token.ritz.config;

import com.rit.token.ritz.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final ApplicationAuthFilter applicationAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, ApplicationAuthFilter applicationAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.applicationAuthFilter = applicationAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.FORWARD, jakarta.servlet.DispatcherType.ERROR).permitAll()
                // Static web resources & SPA routes
                .requestMatchers("/", "/index.html", "/dev-portal", "/dev-portal/**", "/admin-portal", "/admin-portal/**", "/assets/**", "/favicon.ico", "/*.svg", "/*.png", "/*.ico").permitAll()

                // Public authentication and payment callback endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/dev-portal/login").permitAll()
                .requestMatchers("/api/v1/payments/webhook").permitAll()
                .requestMatchers("/api/v1/payments/config").permitAll()
                
                // OpenAPI Swagger UI and Actuator Health
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasAnyRole("SUPER_ADMIN")

                // Application Integration Endpoints (Tillo, Campus ERP, etc.)
                .requestMatchers("/api/v1/transactions/**").hasAnyRole("APPLICATION", "SUPER_ADMIN", "FINANCE_ADMIN")
                .requestMatchers("/api/v1/students/**").hasAnyRole("APPLICATION", "SUPER_ADMIN", "FINANCE_ADMIN", "STUDENT")
                .requestMatchers("/api/v1/tokens/**").hasAnyRole("APPLICATION", "SUPER_ADMIN", "FINANCE_ADMIN")

                // Student Wallet & Notification Operations
                .requestMatchers("/api/v1/wallet/**").authenticated()
                .requestMatchers("/api/v1/notifications/**").authenticated()
                .requestMatchers("/api/v1/refunds/request").hasRole("STUDENT")

                // Merchant Dashboard
                .requestMatchers("/api/v1/merchants/**").hasAnyRole("MERCHANT_ADMIN", "FINANCE_ADMIN", "SUPER_ADMIN")

                // Administrative, Reports, Auditing & Application Management
                .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "FINANCE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/applications/**").authenticated()
                .requestMatchers("/api/v1/applications/**").hasAnyRole("SUPER_ADMIN", "APP_DEVELOPER")
                .requestMatchers("/api/v1/reports/**", "/api/v1/reconciliation/**", "/api/v1/audit/**", "/api/v1/ledger/**").hasAnyRole("SUPER_ADMIN", "FINANCE_ADMIN")

                // Any other request must be authenticated
                .anyRequest().authenticated()
            )
            .addFilterBefore(applicationAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-API-KEY", "X-API-SECRET", "Idempotency-Key", "X-Razorpay-Signature"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition", "Idempotency-Key"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
