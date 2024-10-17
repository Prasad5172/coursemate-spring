package com.intern.coursemate.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Data;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.intern.coursemate.exception.TokenExpired;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

@Service
@Data
public class JwtService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;



    public Mono<String> generateToken(UserDetails userDetails) {
        return  generateToken(new HashMap<>(), userDetails);
    }

    public Mono<String> generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Mono.just(buildToken(extraClaims, userDetails, jwtExpiration));
    }
    public String buildToken(
        Map<String, Object> extraClaims,
        UserDetails userDetails,
        long expiration
    ) {
        return Jwts.builder()
                .claims()
                .add(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .and()
                .signWith(getKey())
                .compact();

    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Mono<String> extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> Mono<T> extractClaim(String token, Function<Claims, T> claimsResolver) {
        return extractAllClaims(token)
                .map(claimsResolver::apply);
    }

    private Mono<Claims> extractAllClaims(String token) {
        return Mono.fromCallable(() -> {
            try {
                return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            } catch (ExpiredJwtException e) {
                throw new TokenExpired("Token expired");
            } catch (SignatureException e) {
                throw new SignatureException("Invalid JWT signature");
            } catch (MalformedJwtException e) {
                throw new MalformedJwtException("Invalid JWT token");
            } catch (UnsupportedJwtException e) {
                throw new UnsupportedJwtException("JWT token is unsupported");
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("JWT claims string is empty");
            }
        });
    }

    public Mono<Boolean> isTokenValid(String token, UserDetails userDetails) {
        return  extractUserName(token).flatMap(userName -> {
            if (userName.equals(userDetails.getUsername())) {
                return isTokenExpired(token).map(expired -> !expired);
            }
            return Mono.just(false);
        });
    }

    private Mono<Boolean> isTokenExpired(String token) {
        return extractExpiration(token)
                .map(expirationDate -> expirationDate.before(new Date()));
    }

    private Mono<Date> extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}