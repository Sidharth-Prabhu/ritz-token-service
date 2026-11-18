package com.rit.token.ritz.config;

import com.rit.token.ritz.application.ApplicationService;
import com.rit.token.ritz.application.RegisteredApplication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class ApplicationAuthFilter extends OncePerRequestFilter {

    @Value("${ritz.api.key:ritz-secret-api-key-2026}")
    private String masterApiKey;

    private final ApplicationService applicationService;

    public ApplicationAuthFilter(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // If already authenticated via JWT, proceed
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-KEY");
        String apiSecret = request.getHeader("X-API-SECRET");

        // Also check Authorization header for Bearer <apiKey> if not JWT
        if (apiKey == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ") && !authHeader.contains(".")) {
                apiKey = authHeader.substring(7);
            }
        }

        if (apiKey != null && !apiKey.isBlank()) {
            // 1. Check legacy master API key
            if (masterApiKey.equals(apiKey)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "LEGACY_MASTER_CLIENT",
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_APPLICATION"))
                );
                request.setAttribute("authenticatedApplicationId", "MASTER_INTEGRATION");
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // 2. Check registered applications in database
                Optional<RegisteredApplication> appOpt = applicationService.validateCredentials(apiKey, apiSecret);
                if (appOpt.isPresent()) {
                    RegisteredApplication app = appOpt.get();
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            app.getApplicationId(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_APPLICATION"))
                    );
                    request.setAttribute("authenticatedApplicationId", app.getApplicationId());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
