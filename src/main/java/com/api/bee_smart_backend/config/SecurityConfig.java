package com.api.bee_smart_backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS using the configuration source
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/lessons/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/grades/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/topics/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/parent/**").hasAnyAuthority(RolePermissions.ALL_API_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/users/user-info").hasAnyAuthority(RolePermissions.ALL_API_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/questions/**").hasAnyAuthority(RolePermissions.ALL_API_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAnyAuthority(RolePermissions.ALL_API_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAnyAuthority(RolePermissions.ALL_API_ROLES)
                        .requestMatchers("/api/statistics/client/**").hasAnyAuthority(RolePermissions.ALL_API_ROLES)
                        .requestMatchers("/api/statistics/admin/**").hasAnyAuthority(RolePermissions.ADMIN_ROLES)
                        .requestMatchers("/api/users/**").hasAnyAuthority(RolePermissions.ADMIN_ROLES)
                        .requestMatchers("/api/customers/**").hasAnyAuthority(RolePermissions.ADMIN_ROLES)
                        .requestMatchers("/api/lessons/**").hasAnyAuthority(RolePermissions.ADMIN_ROLES)
                        .requestMatchers("/api/book-types/**").hasAnyAuthority(RolePermissions.ADMIN_ROLES)
                        .requestMatchers("/api/subjects/**").hasAnyAuthority(RolePermissions.ADMIN_ROLES)
                        .requestMatchers("/api/grades/**").hasAnyAuthority(RolePermissions.ADMIN_ROLES)
                        .requestMatchers("/api/topics/**").hasAnyAuthority(RolePermissions.ADMIN_ROLES)
                        .requestMatchers("/api/questions/**").hasAnyAuthority(RolePermissions.ADMIN_ROLES)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((HttpServletRequest request, HttpServletResponse response,
                                                   org.springframework.security.core.AuthenticationException authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.getWriter().write("Unauthorized: " + authException.getMessage());
                        })
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(jwtUserDetailsService);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000")); // Specify your frontend URL
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH")); // Allowed HTTP methods
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type")); // Allowed headers
        configuration.setAllowCredentials(true); // Allow credentials (cookies, authorization headers)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS configuration globally
        return source;
    }
}