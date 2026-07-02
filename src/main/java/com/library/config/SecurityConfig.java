package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Spring Security configuration: role-based access control to the API/URLs.
 *
 * <p>Authentication itself is performed by {@code AuthController} (email + BCrypt via
 * {@code UserService}); on success it populates the Spring Security context so these
 * authorization rules and {@code @PreAuthorize} annotations can enforce roles.
 *
 * <p>Access map:
 * <ul>
 *   <li>public: /login, /register, /logout, static resources</li>
 *   <li>/admin/**      → ADMIN</li>
 *   <li>/librarian/**  → LIBRARIAN or ADMIN (view); write ops further restricted via @PreAuthorize</li>
 *   <li>everything else → any authenticated user</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection is ON. Thymeleaf (thymeleaf-spring5) automatically injects the
            // CSRF token into every <form th:action="..."> as a hidden field.
            .csrf()
            .and()
            .authorizeRequests()
                .antMatchers("/login", "/register", "/logout",
                        "/css/**", "/js/**", "/images/**", "/error").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/librarian/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                // Not logged in → go to the login page.
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                // Logged in but wrong role → send home (HomeController routes by role).
                .accessDeniedHandler((request, response, ex) ->
                        response.sendRedirect(request.getContextPath() + "/"))
            .and()
            // Logout is handled by AuthController, not by Spring Security.
            .logout().disable();
        return http.build();
    }
}
