package com.intern.coursemate.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private ReactiveAuthenticationManager authenticationManager;

    // @Autowired
    // @Qualifier("delegatedAuthenticationEntryPoint")
    // AuthenticationEntryPoint authEntryPoint;

    // CORS configuration for reactive environment
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Security filter chain in a reactive environment using ServerHttpSecurity
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/auth/**").permitAll() // Exclude auth endpoints from authentication
                .anyExchange().authenticated() // Any other request needs to be authenticated
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS with config
            .authenticationManager(authenticationManager) // Reactive AuthenticationManager
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance()) // Disable session management
            .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION) // Use JWT filter
            .exceptionHandling(exceptionHandling -> 
                exceptionHandling.authenticationEntryPoint((exchange, e) -> Mono.error(new RuntimeException("Unauthenticated!")))
            );
        return http.build();
    }

}
