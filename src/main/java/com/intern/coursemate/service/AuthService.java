package com.intern.coursemate.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.AssertingParty.Verification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.intern.coursemate.dao.UserRepository;
import com.intern.coursemate.dto.LoginDto;
import com.intern.coursemate.dto.UserDto;
import com.intern.coursemate.dto.VerifyDto;
import com.intern.coursemate.email.EmailService;
import com.intern.coursemate.model.User;

import reactor.core.publisher.Mono;

import java.util.*;
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthenticationManager authenticationManager;
    
    public Mono<User> findUserById(int id) {
        return userRepository.findById(id);
    }

    

    public User signup(UserDto user){
        try {
            
            User newUser = User.builder().name(user.getName()).email(user.getEmail()).password(passwordEncoder.encode(user.getPassword())).build();
            newUser.setVerificationCode(getVerificationCode());
            newUser.setCodeExpiredAt(LocalDateTime.now().plusMinutes(15));
            userRepository.save(newUser);
            emailService.send(user.getEmail(), buildVerificationEmail(user.getEmail(), newUser.getVerificationCode()), "Verification" );
            return newUser;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public User login(LoginDto loginDto) {
        Mono<User> user = userRepository.findByEmail(loginDto.getEmail());
        if(!user.isEnabled()){
            throw new RuntimeException("email is not verified");
        }
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));
        return user;
    }


    public Mono<Void> verifyEmail(VerifyDto verifyDto){
        return userRepository.findByEmail(verifyDto.getEmail())
                .switchIfEmpty(Mono.error(new RuntimeException("user not found exception")))
                .flatMap(user -> {
                    if(user.getCodeExpiredAt().isBefore(LocalDateTime.now())){
                        Mono.error(new RuntimeException("verification code expired"));
                    }
                    if(!user.getVerificationCode().equals(verifyDto.getVerificationCode())){
                        Mono.error(new RuntimeException("Invalid verification code"));
                    }
                    user.setIsVerified(true);
                    user.setVerificationCode(null);
                    user.setCodeExpiredAt(null);
                    return userRepository.save(user).then(Mono.empty());
                });   
    }
    public Mono<Void> resendCode(String email){
        return userRepository.findByEmail(email)
        .switchIfEmpty(Mono.error(new RuntimeException("user not found exception")))
        .flatMap(user -> {
            if(user.isEnabled()){
                Mono.error(new RuntimeException("Account is already verified"));
            }
            return Mono.just(user);
        }).flatMap(user -> {
            String code = getVerificationCode();
            user.setVerificationCode(code);
            user.setCodeExpiredAt(LocalDateTime.now().plusMinutes(10));
            return emailService.send(email, buildVerificationEmail(email, code), "Verification Code").then(userRepository.save(user)).then();
        });       
    }

    private String getVerificationCode(){
        Random random = new Random();
        int code = random.nextInt(900000)+100000;

        return String.valueOf(code);
    }

    private String buildVerificationEmail(String userEmail, String verificationCode) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "<p>Dear " + userEmail + ",</p>\n" +
                "<p>Thank you for registering with our service. To complete your registration, please use the verification code below:</p>\n" +
                "<h2 style=\"background-color:#1D70B8;color:white;padding:10px 20px;text-align:center;\">" + verificationCode + "</h2>\n" +
                "<p>This code is valid for the next 15 minutes.</p>\n" +
                "<p>If you did not request this code, please ignore this email.</p>\n" +
                "<p>If you encounter any issues, feel free to contact our support team.</p>\n" +
                "<p>Best regards,</p>\n" +
                "<p>Your Company</p>\n" +
                "</div>";
    }
}
