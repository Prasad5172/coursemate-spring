package com.intern.coursemate.controller;

import java.io.IOException;
import java.lang.StackWalker.Option;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intern.coursemate.dao.DocumentRepository;
import com.intern.coursemate.email.EmailService;
import com.intern.coursemate.exception.CustomException;
import com.intern.coursemate.model.Document;
import com.intern.coursemate.request.DocumentRequest;
import com.intern.coursemate.service.DocumentService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/docs")
@Slf4j
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EmailService emailService;
    

    @PostMapping("/upload")
    public Mono<ResponseEntity<String>> uploadFile( @RequestParam("file") MultipartFile file,@RequestParam("documentRequest") String documentRequestJson ) {
        ObjectMapper objectMapper = new ObjectMapper();
        return Mono.fromCallable(() -> objectMapper.readValue(documentRequestJson, DocumentRequest.class))
        .flatMap(documentRequest -> {
            return documentService.uploadFile(file)
                .flatMap(fileName -> {
                    String fileUrl = "https://rguktcoursemate.s3.eu-north-1.amazonaws.com/" + fileName;
                    Document document = Document.builder()
                            .name(fileName)
                            .sem(documentRequest.getSem())
                            .year(documentRequest.getYear())
                            .subject(documentRequest.getSubject())
                            .url(fileUrl)
                            .build();
                    return documentRepository.save(document)
                        .flatMap(savedDocument -> {
                            // Send the email reactively
                            String verifyUrl = "http://localhost:8080/docs/verify?id=" + savedDocument.getId();
                            String rejectUrl = "http://localhost:8080/docs/reject?id=" + savedDocument.getId() + "&name=" + fileName;

                            return emailService.send("n200072@rguktn.ac.in", 
                                                     buildDocumentEmail(fileName, fileUrl, verifyUrl, rejectUrl), 
                                                     "Verify document")
                                .thenReturn(new ResponseEntity<>(fileUrl, HttpStatus.OK));
                        });
                });
        })
        .onErrorResume(IOException.class, e -> {
            log.error("IOException occurred during file upload: {}", e.getMessage());
            return Mono.just(new ResponseEntity<>("File upload failed due to an IO error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        })
        .onErrorResume(e -> {
            log.error("Error occurred during file upload: {}", e.getMessage());
            return Mono.just(new ResponseEntity<>("File upload failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        });
}

    @GetMapping("/fetch")
    public Flux<Document> getMethodName(@RequestBody Document document) {
        return  documentRepository.fetchDocumentsByQuery(document.getYear(),document.getSem(), document.getSubject());
    }

    @GetMapping("/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> verify(@RequestParam("id") int id) {
        return documentRepository.findById(id).switchIfEmpty(Mono.error(new CustomException("Document with ID " + id + " not found")))
        .flatMap(doc -> {
            doc.setVerified(true);
            return documentRepository.save(doc).thenReturn("verified");
        });
        
    }
    @GetMapping("/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<String>> reject(@RequestParam("id") String id) {
        return documentRepository.findById(Integer.parseInt(id))
                .switchIfEmpty(Mono.error(new CustomException("Document with ID " + id + " not found")))
                .flatMap(docu -> 
                    emailService.send("prasadpadala2005@gmail.com", buildRejectionEmail(docu.getName(), docu.getUrl(), "", docu.getSem(), docu.getYear(), docu.getSubject()) , "Document Rejected")
                    .then(documentRepository.delete(docu))
                    .thenReturn(ResponseEntity.status(HttpStatus.OK).body("deleted"))
                ).onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage())));   
    }

    private String buildDocumentEmail(String documentName, String documentUrl, String verificationLink, String rejectionLink) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "<p>Dear User,</p>\n" +
                "<p>Please review the following document: <b>" + documentName + "</b></p>\n" +
                "<p>You can view the document by clicking the link below:</p>\n" +
                "<p><a href=\"" + documentUrl + "\" style=\"color:#1D70B8;text-decoration:none\">View Document</a></p>\n" +
                "\n" +
                "<p>After reviewing the document, please take one of the following actions:</p>\n" +
                "\n" +
                "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin-top:20px\">\n" +
                "  <tr>\n" +
                "    <td>\n" +
                "      <a href=\"" + verificationLink + "\" style=\"background-color:#1D70B8;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;font-size:16px;\">Verify</a>\n" +
                "    </td>\n" +
                "    <td style=\"padding-left:20px\">\n" +
                "      <a href=\"" + rejectionLink + "\" style=\"background-color:#FF4C4C;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;font-size:16px;\">Reject</a>\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "</table>\n" +
                "\n" +
                "<p>Thank you for your attention.</p>\n" +
                "<p>Best regards,</p>\n" +
                "<p>Your Company</p>\n" +
                "</div>";
    }
    

    private String buildRejectionEmail(String documentName, String documentUrl, String uploadLink, int sem, String year, String subject) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "<p>Dear User,</p>\n" +
                "<p>We regret to inform you that the following document has been <b>rejected</b>:</p>\n" +
                "<p><b>Document Name:</b> " + documentName + "</p>\n" +
                "<p><b>Semester:</b> " + sem + "</p>\n" +
                "<p><b>Year:</b> " + year + "</p>\n" +
                "<p><b>Subject:</b> " + subject + "</p>\n" +
                "<p>You can review the document by clicking the link below:</p>\n" +
                "<p><a href=\"" + documentUrl + "\" style=\"color:#1D70B8;text-decoration:none\">View Document</a></p>\n" +
                "\n" +
                "<p>To resolve this, please verify the document details and re-upload it with the correct information.</p>\n" +
                "<p>Use the link below to upload the corrected document:</p>\n" +
                "<p><a href=\"" + uploadLink + "\" style=\"background-color:#1D70B8;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;font-size:16px;\">Upload Corrected Document</a></p>\n" +
                "\n" +
                "<p>If you need further assistance, feel free to reach out.</p>\n" +
                "<p>Thank you for your cooperation.</p>\n" +
                "<p>Best regards,</p>\n" +
                "<p>Your Company</p>\n" +
                "</div>";
    }
    
    
    

    
    

}
