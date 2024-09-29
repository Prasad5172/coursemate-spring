package com.intern.coursemate.email;

public interface EmailSender {
    void send(String to , String body,String subject);
}
