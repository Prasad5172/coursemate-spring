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
import com.intern.coursemate.dto.FileUploadResponse;
import com.intern.coursemate.email.EmailService;
import com.intern.coursemate.model.Document;
import com.intern.coursemate.request.DocumentRequest;
import com.intern.coursemate.request.DocumentVerificationRequest;
import com.intern.coursemate.service.DocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/docs")
@Slf4j
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRespositry;

    @Autowired
    private EmailService emailService;
    

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile( @RequestParam("file") MultipartFile file,
    @RequestParam("documentRequest") String documentRequestJson ) throws AmazonServiceException, SdkClientException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        DocumentRequest documentRequest = objectMapper.readValue(documentRequestJson, DocumentRequest.class);
        log.info("Upload request for subject: {}, year: {}, semester: {}", 
        documentRequest.getSubject(), documentRequest.getYear(), documentRequest.getSem());

        // Handle the file upload
        String fileName = documentService.uploadFile(file);
        String fileUrl = "https://rguktcoursemate.s3.eu-north-1.amazonaws.com/" + fileName;
        Document document = Document.builder().name(fileName).isVerified(false).sem(documentRequest.getSem()).year(documentRequest.getYear()).subject(documentRequest.getSubject()).url(fileUrl).build();
        documentRespositry.save(document);
        emailService.send("n200072@rguktn.ac.in",buildDocumentEmail(fileName, fileUrl, "http://localhost:8080/docs/verify?id="+document.getId() , "http://localhost:8080/docs/reject?id="+document.getId()+"&name="+fileName), "Verify document");
        return new ResponseEntity<>(fileUrl, HttpStatus.OK);
    }

    @GetMapping("/fetch")
    public List<Document> getMethodName(@RequestBody Document document) {
        List<Document> documents = documentRespositry.fetchDocumentsByQuery(document.getYear(),document.getSem(), document.getSubject());
        return documents;
    }

    @GetMapping("/verify")
    public String verify(@RequestParam("id") int id) {
        Optional<Document> document = documentRespositry.findById(id);
        document.ifPresent(doc -> {
            doc.setVerified(true);  // Assuming there's a setter for isVerified
            documentRespositry.save(doc);  // Save the updated document
        });
        return "verified";
    }
    @GetMapping("/reject")
    public String reject(@RequestParam("id") String id) {
        Optional<Document> document = documentRespositry.findById(Integer.parseInt(id));
        document.ifPresent(docu -> {
            emailService.send("prasadpadala2005@gmail.com", buildRejectionEmail(docu.getName(), docu.getUrl(), "", docu.getSem(), docu.getYear(), docu.getSubject()) , "Document Rejected");
            // documentService.deleteFile(docu.getName());
            documentRespositry.deleteById(docu.getId());
        });
        // need to send email to the document uploader
        return "delete";
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
