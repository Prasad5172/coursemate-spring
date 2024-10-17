package com.intern.coursemate.email;

import reactor.core.publisher.Mono;

public interface EmailSender {
    Mono<Void> send(String to , String body,String subject);
}
