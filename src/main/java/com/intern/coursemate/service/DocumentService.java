package com.intern.coursemate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.intern.coursemate.dao.DocumentRepository;
import com.intern.coursemate.dao.UserRepository;
import com.intern.coursemate.model.Document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.file.FileSystemException;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {
    
    @Value("${appilication.bucket.name}")
    private String bucketName;

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Autowired
    private AmazonS3 s3Client;


    public Mono<String> uploadFile(MultipartFile multipartFile) {
        return Mono.fromCallable(() -> {
            String fileName = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(multipartFile.getContentType());
            objectMetadata.setContentLength(multipartFile.getSize());
            s3Client.putObject(bucketName, fileName, multipartFile.getInputStream(), objectMetadata);
            return fileName;
        }).onErrorResume(e ->  Mono.error(new FileSystemException("Error occurred in file upload ==> "+e.getMessage())));
        
    }

    public Mono<String> deleteFile(String fileName) {
        return Mono.fromRunnable(() -> s3Client.deleteObject(bucketName,fileName)).thenReturn(fileName + " removed ...");
    }

    public Flux<Document> getDocumentsByUserEmail(String email) {
        return userRepository.findByEmail(email)
                .flatMapMany(user -> documentRepository.findByUserId(user.getId()));
    }
    

    
}
