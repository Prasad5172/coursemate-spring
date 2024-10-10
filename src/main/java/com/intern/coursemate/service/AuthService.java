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
    
    public Optional<User> findUserById(int id) {
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
        User user = userRepository.findByEmail(loginDto.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if(!user.isEnabled()){
            throw new RuntimeException("email is not verified");
        }
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));
        return user;
    }


    public void verifyEmail(VerifyDto verifyDto){
        Optional<User> user = userRepository.findByEmail(verifyDto.getEmail());
        
        if (user.isPresent()) {
            User _user = user.get();
            
            if (_user.getCodeExpiredAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code expired.");
            }
            
            if (_user.getVerificationCode().equals(verifyDto.getVerificationCode())) {
                _user.setIsVerified(true);
                _user.setVerificationCode(null);
                _user.setCodeExpiredAt(null);
                userRepository.save(_user);
            } else {
                throw new RuntimeException("Invalid verification code.");
            }
        } else {
            throw new RuntimeException("User not found.");
        }
        
    }
    public void resendCode(String email){
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            User _user = user.get();
            if(_user.isEnabled()){
                throw new RuntimeException("Account is already verified");
            }
            _user.setVerificationCode(getVerificationCode());
            _user.setCodeExpiredAt(LocalDateTime.now().plusMinutes(10));
            emailService.send(email, buildVerificationEmail(email, _user.getVerificationCode()), "Verification Code");
            userRepository.save(_user);
        }else{
            throw new RuntimeException("User not found exception");
        }
        
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
