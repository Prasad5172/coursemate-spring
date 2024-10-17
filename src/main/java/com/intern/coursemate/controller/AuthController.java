package com.intern.coursemate.controller;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.GetMapping;
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
    public String googleLogin(@RequestBody Map<String, String> request) throws Exception {
        System.out.println("google-login");
        String token = request.get("token");
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList("161391512148-7jk6os4v8ado31rcbupprcbhue6l2p2j.apps.googleusercontent.com"))
                .build();
        GoogleIdToken idToken = verifier.verify(token);
        
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            // String userId = payload.getSubject();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
            String name = (String) payload.get("name");
            Mono<User> user = userRepository.findByEmail(email);
            User _user;
            if(!user.isPresent()){
                _user = User.builder().email(email).name(name).isVerified(emailVerified).build();
                userRepository.save(_user);
            }else{
                _user = user.get();
            }
            // Handle successful authentication, store user info, etc.
            return jwtService.generateToken(_user);
        } else {
            throw new RuntimeException("Invalid ID token.");
        }
    }
    @PostMapping("/signup")
    public ResponseEntity<User> register(@RequestBody @Valid UserDto userDto) throws Exception {
        User  registredUser = authService.signup(userDto);

        return ResponseEntity.ok(registredUser);
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginDto loginDto)  {
            System.out.println("login");
            User  registredUser = authService.login(loginDto);
            String jwt = jwtService.generateToken(registredUser);
            LoginResponse loginResponse = new LoginResponse(jwt,0);
            return ResponseEntity.ok(loginResponse);
       
    }
    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody @Valid VerifyDto verifyDto) throws Exception {
        try {
            authService.verifyEmail(verifyDto);
            return ResponseEntity.ok("Account verified Succesfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); 
        }
    }
    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestParam String  email)   {
        try {
            authService.resendCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); 
        }
    }

    
    
}
