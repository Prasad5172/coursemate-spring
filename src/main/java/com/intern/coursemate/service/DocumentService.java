package com.intern.coursemate.service;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Service
@Slf4j
public class DocumentService {
    
    @Value("${appilication.bucket.name}")
    private String bucketName;

    @Autowired
    private AmazonS3 s3Client;


    public String uploadFile(MultipartFile multipartFile) throws AmazonServiceException, SdkClientException, IOException {
        String fileName = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(multipartFile.getContentType());
            objectMetadata.setContentLength(multipartFile.getSize());
            s3Client.putObject(bucketName, fileName, multipartFile.getInputStream(), objectMetadata);
            return  fileName;
        } catch (Exception e) {
            log.error("Error occurred ==> {}", e.getMessage());
            throw new FileUploadException("Error occurred in file upload ==> "+e.getMessage());
        }
    }

    public String deleteFile(String fileName) {
        s3Client.deleteObject(bucketName, fileName);
        return fileName + " removed ...";
    }
    

    
}
