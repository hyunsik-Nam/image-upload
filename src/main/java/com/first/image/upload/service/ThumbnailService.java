package com.first.image.upload.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.first.image.upload.entity.Image;
import com.first.image.upload.entity.ThumbnailStatus;
import com.first.image.upload.repo.ImageRepository;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class ThumbnailService {
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private MinioService minioService;
    
    @Async("thumbnailExecutor")
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void generateThumbnailAsync(Long imageId) throws IOException {
        try {
            log.info("Starting thumbnail generation for image ID: {}", imageId);
            
            // 1. 원본 이미지 정보 조회
            Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with ID: " + imageId));
            
            // 2. 원본 이미지 다운로드
            byte[] originalImageData = minioService.downloadImage(image.getFileName());
            
            // 3. 썸네일 생성 (150x150)
            byte[] thumbnailData = createThumbnail(originalImageData, 150, 150);
            
            // 4. 썸네일 S3 업로드
            String thumbnailFileName = generateThumbnailFileName(image.getProjectId(), image.getFileName());
            minioService.uploadMinioImage(thumbnailFileName, thumbnailData, "image/jpeg");
            
            // 5. DB 상태 업데이트
            image.setThumbnailFileName(thumbnailFileName);
            image.setThumbnailStatus(ThumbnailStatus.READY);
            imageRepository.save(image);
            
            log.info("Thumbnail generation completed for image ID: {}", imageId);
            
        } catch (Exception e) {
            log.error("Thumbnail generation failed for image ID: {}", imageId, e);
            handleThumbnailFailure(imageId, e);
            throw e; // Retry를 위해 예외를 다시 던짐
        }
    }
    
    private byte[] createThumbnail(byte[] originalImageData, int width, int height) throws IOException {
        // 원본 이미지 읽기
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalImageData));
        
        // 썸네일 크기 계산 (비율 유지)
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        double ratio = Math.min((double) width / originalWidth, (double) height / originalHeight);
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        // 썸네일 이미지 생성
        BufferedImage thumbnailImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnailImage.createGraphics();
        
        // 고품질 렌더링 설정
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        // byte 배열로 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnailImage, "jpg", baos);
        return baos.toByteArray();
    }
    
    private String generateThumbnailFileName(String projectId, String originalFileName) {
        String nameWithoutExtension = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        return nameWithoutExtension + "_thumbnail.jpg";
    }
    
    private void handleThumbnailFailure(Long imageId, Exception e) {
        try {
            Image image = imageRepository.findById(imageId).orElseThrow();
            image.setRetryCount(image.getRetryCount() + 1);
            
            if (image.getRetryCount() >= 3) {
                image.setThumbnailStatus(ThumbnailStatus.FAILED);
                log.error("Thumbnail generation permanently failed for image ID: {} after 3 attempts", imageId);
            }
            
            imageRepository.save(image);
        } catch (Exception saveException) {
            log.error("Failed to update thumbnail failure status for image ID: {}", imageId, saveException);
        }
    }
}