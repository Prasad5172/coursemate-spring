package com.intern.coursemate.email;


import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Service
@RequiredArgsConstructor
public class EmailService implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    public Mono<Void> send(String toEmail, String body,String subject) {
        return Mono.fromRunnable(() -> {
                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
                    helper.setText(body, true);
                    helper.setSubject(subject);
                    helper.setFrom("n200072@rguktn.ac.in");
                    helper.setTo(toEmail);
                    mailSender.send(mimeMessage); // This is a blocking operation
                } catch (MessagingException e) {
                    throw new RuntimeException("Failed to send email", e);
                }
    }).subscribeOn(Schedulers.boundedElastic()).then();
    }

}
