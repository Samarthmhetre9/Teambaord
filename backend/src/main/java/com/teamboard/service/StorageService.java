package com.teamboard.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Stores attachments in S3 when S3_BUCKET is set (private bucket, files are served through short-lived
 * presigned URLs). Without S3_BUCKET it falls back to ./uploads so the app runs with zero cloud setup.
 */
@Service
public class StorageService {
    @Value("${app.storage.s3-bucket:}") private String bucket;
    @Value("${app.storage.region}") private String region;
    @Value("${app.storage.local-dir}") private String localDir;
    @Value("${app.public-url}") private String publicUrl;

    private S3Client s3;
    private S3Presigner presigner;

    @PostConstruct
    void init() throws IOException {
        if (useS3()) {
            s3 = S3Client.builder().region(Region.of(region)).build();            // credentials: default AWS chain
            presigner = S3Presigner.builder().region(Region.of(region)).build();
        } else {
            Files.createDirectories(Path.of(localDir));
        }
    }

    public boolean useS3() { return bucket != null && !bucket.isBlank(); }

    public String store(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "file" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String key = UUID.randomUUID() + "-" + original.replaceAll("[^A-Za-z0-9._-]", "_");
        if (useS3()) {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(file.getContentType()).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } else {
            Files.copy(file.getInputStream(), Path.of(localDir).resolve(key), StandardCopyOption.REPLACE_EXISTING);
        }
        return key;
    }

    public String downloadUrl(Long attachmentId, String key, String fileName) {
        if (!useS3()) return publicUrl + "/api/attachments/" + attachmentId + "/file";
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key)
                .responseContentDisposition("attachment; filename=\"" + fileName.replace("\"", "") + "\"").build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)).getObjectRequest(get).build()).url().toString();
    }

    public Path localPath(String key) { return Path.of(localDir).resolve(key).normalize(); }
}
