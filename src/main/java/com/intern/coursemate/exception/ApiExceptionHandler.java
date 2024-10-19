package com.intern.coursemate.exception;



import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import reactor.core.publisher.Mono;


@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleSecurityException(Exception ex) {
        System.out.println("ExceptionHandler");
        System.out.println(ex);

        ProblemDetail errorDetail;
        HttpStatus status;

        if (ex instanceof BadCredentialsException) {
            status = HttpStatus.UNAUTHORIZED;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Authentication failure");
        } else if (ex instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Not Authorized");
        } else if (ex instanceof MethodArgumentNotValidException) {
            status = HttpStatus.BAD_REQUEST;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Enter Valid Details");
        } else if (ex instanceof SignatureException) {
            status = HttpStatus.BAD_REQUEST;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Enter Valid Details");
        } else if (ex instanceof ExpiredJwtException) {
            status = HttpStatus.FORBIDDEN;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Jwt token is expired");
        } else if (ex instanceof MalformedJwtException) {
            status = HttpStatus.FORBIDDEN;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Not a valid jwt");
        } else if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Not a valid jwt");
        } else if (ex instanceof UsernameNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Email not found");
        } else if (ex instanceof CustomException) {
            status = HttpStatus.BAD_REQUEST;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Email is already registered");
        } else if (ex instanceof TokenExpired) {
            status = HttpStatus.BAD_REQUEST;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Some thing went wrong");
        } else if (ex instanceof HttpMessageNotReadableException) {
            status = HttpStatus.BAD_REQUEST;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Must pass required parameters");
        } else if (ex instanceof InsufficientAuthenticationException) {
            status = HttpStatus.FORBIDDEN;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Must be authenticated");
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            errorDetail.setProperty("Reason", "Internal Server Error");
        }

        return Mono.just(ResponseEntity.status(status).body(errorDetail));
    }
}

