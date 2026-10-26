package com.nynaromanoff.product_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file) {
        log.info("[S3] Iniciando upload do arquivo: {}", file.getOriginalFilename());

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uniqueFileName = UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String publicUrl;
            if (endpoint != null && !endpoint.isBlank()) {
                String normalizedEndpoint = endpoint.replaceAll("/+$", "");
                publicUrl = normalizedEndpoint + "/" + bucketName + "/" + uniqueFileName;
            } else {
                publicUrl = "https://" + bucketName + ".s3." + region
                        + ".amazonaws.com/" + uniqueFileName;
            }

            log.info("[S3] Upload concluído: {}", publicUrl);
            return publicUrl;
        } catch (IOException e) {
            log.error("[S3] Falha ao ler arquivo {}", originalFilename, e);
            throw new RuntimeException("Erro ao processar o arquivo para upload", e);
        } catch (Exception e) {
            log.error("[S3] Falha ao enviar arquivo para S3/LocalStack", e);
            throw new RuntimeException("Falha na comunicação com o S3/LocalStack", e);
        }
    }

    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String fileKey = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(request);
            log.info("[S3] Imagem removida: {}", fileKey);
        } catch (Exception e) {
            log.error("[S3] Erro ao remover arquivo {}", imageUrl, e);
        }
    }
}
