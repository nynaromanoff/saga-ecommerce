package com.nynaromanoff.product_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
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

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file) {
        log.info("📤 [S3] Iniciando processamento de upload para o arquivo: {}", file.getOriginalFilename());

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(file.getContentType())
                    .build();


            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String publicUrl = "https://" + bucketName + ".s3." + region + "://" + uniqueFileName;            log.info("✅ [S3] Arquivo disponibilizado na nuvem com sucesso! URL: {}", publicUrl);
            return publicUrl;

        } catch (IOException e) {
            log.error("❌ [S3] Falha crítica de I/O ao tentar ler os bytes do arquivo {}", originalFilename, e);
            throw new RuntimeException("Erro ao processar o arquivo binário para upload", e);
        } catch (Exception e) {
            log.error("❌ [S3] Erro inesperado ao transmitir dados para a API da AWS S3", e);
            throw new RuntimeException("Falha na comunicação com a AWS S3", e);
        }
    }

    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String fileKey = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("🗑️ [S3] Imagem removida com sucesso do storage: {}", fileKey);

        } catch (Exception e) {
            log.error("⚠️ Erro ao tentar remover arquivo do S3/LocalStack: {}", imageUrl, e);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
