package com.intern.coursemate.exception;

public class TokenExpired extends RuntimeException{
    public TokenExpired(String message){
        super(message);
    }
}
