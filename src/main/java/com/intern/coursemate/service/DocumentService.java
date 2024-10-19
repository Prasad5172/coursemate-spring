package com.intern.coursemate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import com.intern.coursemate.dao.DocumentRepository;
import com.intern.coursemate.dao.UserRepository;
import com.intern.coursemate.model.Document;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.ByteBuffer;

@Service
@Slf4j
public class DocumentService {

    @Value("${application.bucket.name}")
    private String bucketName;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private S3AsyncClient s3AsyncClient; // No need for @Autowired, handled by @RequiredArgsConstructor

   
    public Mono<String> uploadFile(FilePart filePart) {
        // Generate a unique file name
        String fileName = System.currentTimeMillis() + "_" + filePart.filename();
    
        // Join the data buffers to calculate the file content length
        return DataBufferUtils.join(filePart.content()) // Combine all DataBuffer chunks into one
                .flatMap(dataBuffer -> {
                    byte[] byteArray = new byte[dataBuffer.readableByteCount()]; // Create a byte array to hold the content
                    dataBuffer.read(byteArray); // Read the content into the byte array
                    ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray); // Wrap the byte array into a ByteBuffer
    
                    // Calculate content length
                    long contentLength = byteArray.length;
    
                    // Prepare the request to upload the file, including content length
                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .contentLength(contentLength) // Set the content length for S3
                            .contentType(filePart.headers().getContentType().toString()) // Get content type from FilePart headers
                            .build();
    
                    AsyncRequestBody requestBody = AsyncRequestBody.fromPublisher(Mono.just(byteBuffer).flux());
    
                    // Release DataBuffer (to prevent memory leaks)
                    DataBufferUtils.release(dataBuffer);
    
                    // Upload file to S3 asynchronously using S3AsyncClient
                    return Mono.fromFuture(s3AsyncClient.putObject(putObjectRequest, requestBody))
                            .map(response -> fileName)
                            .doOnError(e -> log.error("Error occurred while uploading file to S3: {}", e.getMessage()));
                })
                .onErrorResume(e -> {
                    log.error("Error while uploading file: {}", e.getMessage());
                    return Mono.error(e);
                });
    }
    
    public Mono<String> deleteFile(String fileName) {
        System.out.println("deleting form s3 func");
        // Create the DeleteObjectRequest
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();
    
        // Use Mono.fromFuture to perform the delete operation asynchronously
        return Mono.fromFuture(s3AsyncClient.deleteObject(deleteObjectRequest))
                .thenReturn(fileName + " removed ...")
                .doOnError(e -> log.error("Error occurred while deleting file {}: {}", fileName, e.getMessage()));
    }
    
    public Flux<Document> getDocumentsByUserEmail(String email) {
        return userRepository.findByEmail(email)
                .flatMapMany(user -> documentRepository.findByUserId(user.getId()))
                .switchIfEmpty(Flux.empty()) // Return empty Flux if no user or documents found
                .doOnError(e -> log.error("Error occurred while fetching documents for email {}: {}", email, e.getMessage()));
    }
    
}
