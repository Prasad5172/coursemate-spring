package com.intern.coursemate.controller;

import java.util.Collections;
import java.util.Map;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intern.coursemate.dao.UserRepository;
import com.intern.coursemate.dto.LoginDto;
import com.intern.coursemate.dto.UserDto;
import com.intern.coursemate.dto.VerifyDto;
import com.intern.coursemate.model.User;
import com.intern.coursemate.response.LoginResponse;
import com.intern.coursemate.service.JwtService;

import jakarta.validation.Valid;

import com.intern.coursemate.service.AuthService;

import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private  AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Value("spring.security.oauth2.client.registration.google.client-id")
    private String clientId;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/google-login")
    public Mono<String> googleLogin(@RequestBody Map<String, String> request) {
        System.out.println("google-login");
        String token = request.get("token");
    
        return Mono.fromCallable(() -> {
            // Use GoogleIdTokenVerifier in a non-blocking manner
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("161391512148-7jk6os4v8ado31rcbupprcbhue6l2p2j.apps.googleusercontent.com"))
                    .build();
            return verifier.verify(token);
        })
        .flatMap(idToken -> {
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
                String name = (String) payload.get("name");
    
                // Check if user exists in the database, and save if not
                return userRepository.findByEmail(email)
                        .switchIfEmpty(Mono.defer(() -> {
                            // Create a new user if the email doesn't exist
                            User newUser = User.builder()
                                    .email(email)
                                    .name(name)
                                    .isVerified(emailVerified)
                                    .build();
                            return userRepository.save(newUser);
                        }))
                        .flatMap(existingUser -> {
                            // Generate a JWT token for the user
                            return Mono.just(jwtService.generateToken(existingUser,existingUser.getId()));
                        });
            } else {
                return Mono.error(new RuntimeException("Invalid ID token."));
            }
        });
    }
    
    
    @PostMapping("/signup")
    public Mono<ResponseEntity<User>> register(@RequestBody @Valid UserDto userDto) {
        return authService.signup(userDto)
            .map(registeredUser -> ResponseEntity.ok(registeredUser)) // Wrap the User in a ResponseEntity
            .onErrorResume(e -> Mono.error(e)); // Handle errors by returning a bad request response
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody @Valid LoginDto loginDto)  {
        return  authService.login(loginDto).map(registeredUser-> {
            String jwt = jwtService.generateToken(registeredUser,registeredUser.getId());
            LoginResponse loginResponse = new LoginResponse(jwt,0);
            return ResponseEntity.ok(loginResponse);
        });
    }
    @PostMapping("/verify")
    public Mono<ResponseEntity<String>> verifyUser(@RequestBody @Valid VerifyDto verifyDto) {
        return authService.verifyEmail(verifyDto)
            .then(Mono.just(ResponseEntity.ok("Account verified successfully")))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PostMapping("/resend")
    public Mono<ResponseEntity<String>> resend(@RequestParam String email) {
        return authService.resendCode(email)
            .then(Mono.just(ResponseEntity.ok("Verification code sent")))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    
    
}
