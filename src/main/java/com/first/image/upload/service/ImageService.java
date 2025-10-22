package com.first.image.upload.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.first.image.upload.entity.Image;
import com.first.image.upload.repo.ImageRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {
    private final ThumbnailService thumbnailService;

    private final ImageRepository imageRepository;

    private final MinioService minioService;

    private final ConcurrentHashMap<String, ReentrantLock> uploadLocks = new ConcurrentHashMap<>();

    @Transactional
    public String uploadImage(String projectId, MultipartFile file) throws IOException {
        try{
            String fileHash = calculateFileHash(file.getBytes());
            String lockKey = projectId + ":" + fileHash;

            uploadLocks.putIfAbsent(lockKey, new ReentrantLock());
            ReentrantLock lock = uploadLocks.get(lockKey);
            lock.lock();


            try {
                // 3. 중복 파일 존재 여부 확인
                Optional<Image> existingImage = imageRepository.findByProjectIdAndFileHash(projectId, fileHash);
                
                if (existingImage.isPresent()) {
                    Image duplicate = existingImage.get();
                    log.info("✅ 중복 파일 감지 - 기존 파일 반환: {} (해시: {})", duplicate.getFileName(), fileHash);
                    return duplicate.getFileName();
                }
                
                String fileName = generateFileName(projectId, file.getOriginalFilename());
    
                minioService.uploadMinioImage(fileName, file.getBytes(), file.getContentType());
    
                Image savedImage = imageRepository.save(Image.builder()
                    .projectId(projectId)
                    .originalFileName(file.getOriginalFilename())
                    .fileName(fileName)
                    .fileHash(fileHash)
                    .fileSize(file.getSize())
                    .build()
                );
                Long imageId = savedImage.getId();
    
                thumbnailService.generateThumbnailAsync(imageId);
                return fileName;
                
            } finally {
                lock.unlock();
                // 락 메모리 누수 방지
                if (!lock.hasQueuedThreads()) {
                    uploadLocks.remove(lockKey);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            throw new IOException("Failed to upload image", e);
        }
        
    }

    public List<Image> getImagesList(String projectId,int page, int size) {
        return imageRepository.findByProjectId(projectId, PageRequest.of(page - 1, size)).getContent();
    }

    public Image getImageById(Long imageId) {
        return imageRepository.findById(imageId)
            .orElseThrow(() -> new RuntimeException("Image not found with ID: " + imageId));
    }

    public String patchImage(Long imageId, MultipartFile file) throws IOException {
        Image image = imageRepository.findById(imageId)
        .orElseThrow(() -> new RuntimeException("Image not found with ID: " + imageId));
        
        
        String previousFileName = image.getFileName();
        String previousThumbnailFileName = image.getThumbnailFileName();
        String fileName = generateFileName(image.getProjectId(), file.getOriginalFilename());
        String fileHash = calculateFileHash(file.getBytes());
        
        
        minioService.uploadMinioImage(fileName, file.getBytes(), file.getContentType());
        
        image.setOriginalFileName(file.getOriginalFilename());
        image.setFileName(fileName);
        image.setFileHash(fileHash);
        image.setFileSize(file.getSize());
        imageRepository.save(image);

        thumbnailService.generateThumbnailAsync(imageId);
        minioService.deleteImage(previousFileName);
        minioService.deleteImage(previousThumbnailFileName);

        return fileName;
    }

    public void deleteImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
            .orElseThrow(() -> new RuntimeException("Image not found with ID: " + imageId));

        minioService.deleteImage(image.getFileName());
        minioService.deleteImage(image.getThumbnailFileName());

        Optional<Image> imageToDelete = imageRepository.findById(imageId);
        if (imageToDelete.isPresent()) {
            image.setDelYn("Y");
            imageRepository.save(image);
        }
    }



    private String generateFileName(String projectId, String originalFileName) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return String.format("projects/%s/%s%s", projectId, UUID.randomUUID().toString(), extension);
    }

    private String calculateFileHash(byte[] fileBytes) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("해시 계산 실패", e);
        }
    }
}