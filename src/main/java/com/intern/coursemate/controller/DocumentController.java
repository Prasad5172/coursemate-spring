package com.intern.coursemate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intern.coursemate.dao.DocumentRepository;
import com.intern.coursemate.email.EmailService;
import com.intern.coursemate.exception.CustomException;
import com.intern.coursemate.model.Document;
import com.intern.coursemate.request.DocumentRequest;
import com.intern.coursemate.service.DocumentService;
import com.intern.coursemate.service.JwtService;

import io.jsonwebtoken.io.IOException;
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

    @Autowired
    private JwtService jwtService;
    

    @PostMapping("/upload")
    public Mono<ResponseEntity<String>> uploadFile(
        @RequestHeader("Authorization") String authorizationHeader,
            @RequestPart("file") Mono<FilePart> filePartMono,
            @RequestPart("documentRequest") Mono<String> documentRequestJsonMono) {

            // Extract JWT token from the Authorization header
            String token = authorizationHeader.replace("Bearer ", "");
            
            // Extract the userId from the token
            Long userId;
            try {
                userId = jwtService.extractUserId(token);
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Invalid JWT token"));
            }
    
        return documentRequestJsonMono
                .flatMap(documentRequestJson -> {
                    // Deserialize the JSON string to DocumentRequest object
                    ObjectMapper objectMapper = new ObjectMapper();
                    DocumentRequest documentRequest;
                    try {
                        documentRequest = objectMapper.readValue(documentRequestJson, DocumentRequest.class);
                    } catch (IOException e) {
                        return Mono.error(new RuntimeException("Invalid document request JSON"));
                    } catch (JsonMappingException e1) {
                        return Mono.error(new RuntimeException("Invalid document request JSON"));
                    } catch (JsonProcessingException e1) {
                        return Mono.error(new RuntimeException("Invalid document request JSON"));
                    }
                    // Handle file upload and save the document in the database
                    return filePartMono
                            .flatMap(filePart -> {
                                // Upload the file asynchronously
                                return documentService.uploadFile(filePart)
                                        .flatMap(fileName -> {
                                            // Construct file URL and save the document
                                            String fileUrl = "https://rguktcoursemate.s3.eu-north-1.amazonaws.com/" + fileName;
                                            Document document = Document.builder()
                                                    .name(fileName)
                                                    .sem(documentRequest.getSem())
                                                    .year(documentRequest.getYear())
                                                    .subject(documentRequest.getSubject())
                                                    .userId(userId)
                                                    .url(fileUrl)
                                                    .build();
                                             Mono<Document>  savedDocumentMono = documentRepository.save(document);
                                            Mono<Void> sendEmailMono = savedDocumentMono.flatMap(savedDocument -> 
                                            emailService.send(
                                                "n200072@rguktn.ac.in",
                                                buildDocumentEmail(fileName, fileUrl,
                                                        "http://localhost:8080/docs/verify?id=" + savedDocument.getId(),
                                                        "http://localhost:8080/docs/reject?id=" + savedDocument.getId() + "&name=" + fileName),
                                                "Verify document"
                                            )
                                        );
                                        sendEmailMono.subscribe();
                                        return Mono.when(
                                            // sendEmailMono,
                                            savedDocumentMono
                                            )
                                .thenReturn(ResponseEntity.ok(fileUrl));                                            
                                        });
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error occurred: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the document"));
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
    public Mono<ResponseEntity<String>> reject(
            @RequestParam("id") String id,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");

        // Extract the userId from the token
        String toEmail;
        try {
            toEmail = jwtService.extractUserName(token);
            System.out.println(toEmail);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Invalid JWT token"));
        }
        return documentRepository.findById(Integer.parseInt(id))
                .switchIfEmpty(Mono.error(new CustomException("Document with ID " + id + " not found")))
                .flatMap(document -> {
                    // Create a Mono for sending the email
                    Mono<Void> sendEmailMono = emailService.send(toEmail, buildRejectionEmail(document.getName(), document.getUrl(), "", document.getSem(), document.getYear(), document.getSubject()) , "Document Rejected").then();
                    
                    // Call the document service (assume it's an asynchronous operation returning
                    // Mono)
                    Mono<String> documentServiceCallMono = documentService.deleteFile(document.getName());
                    // Create a Mono for deleting the document
                    Mono<Void> deleteDocumentMono = documentRepository.delete(document);
                    sendEmailMono.subscribe();
                    // Combine both tasks using Mono.zip or Mono.when
                    return Mono.when(
                        // sendEmailMono, 
                        documentServiceCallMono,
                        deleteDocumentMono
                         )
                            .then(Mono.just(
                                    ResponseEntity.status(HttpStatus.OK).body("Document deleted and email sent")));
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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
