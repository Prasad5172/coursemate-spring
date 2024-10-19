package com.intern.coursemate.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.intern.coursemate.service.JwtService;

import reactor.core.publisher.Mono;




@Configuration
public class JwtFilter implements WebFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ReactiveUserDetailsService userDetailsService;

    @Override
public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    // Allow requests to "/auth/**" without filtering
    if (path.startsWith("/auth/")) {
        return chain.filter(exchange);
    }

    // Get Authorization header
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    // Extract token if the Authorization header starts with "Bearer "
    final String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;

    // If token is present, extract the username
    if (token != null) {
        String username = jwtService.extractUserName(token); // Extract the username from JWT

        if (username != null) {
            // Use ReactiveUserDetailsService to fetch the user reactively
            return userDetailsService.findByUsername(username)
                .flatMap(userDetails -> {
                    // Validate the token with the user details
                    if (jwtService.isTokenValid(token, userDetails)) {
                        // Create authentication token
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        // Set authentication in reactive security context and proceed with the filter chain
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
                    }
                    // If the token is invalid, continue without setting the authentication
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    // Handle any exceptions reactively
                    return Mono.error(e);
                });
        }
    }

    // If no valid token or username, proceed without authentication
    return chain.filter(exchange);
}
}


