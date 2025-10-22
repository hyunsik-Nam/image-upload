package com.first.image.upload.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.first.image.upload.entity.Image;
import com.first.image.upload.service.ImageService;
import com.first.image.upload.service.ThumbnailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Image Upload", description = "이미지 업로드 및 관리 API")
@RestController
@RequiredArgsConstructor
public class ImageUploadController {
    
    private final ImageService imageService;

    @Operation(summary = "이미지 업로드", description = "특정 프로젝트에 이미지를 업로드하고 썸네일을 비동기 생성합니다.")
    @PostMapping("/project/{projectId}/images")
    public ResponseEntity<?> uploadImage(@PathVariable String projectId, @RequestParam("image") MultipartFile image) {
        try {
            // 1. 원본 이미지 업로드 및 DB 저장
            String imageId = imageService.uploadImage(projectId, image);
            
            // 2. 썸네일 비동기 생성 시작
            // thumbnailService.generateThumbnailAsync(imageId);
            
            return ResponseEntity.ok(Map.of(
                "imageId", imageId,
                "message", "Image uploaded successfully. Thumbnail is being processed.",
                "thumbnailStatus", "PROCESSING"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    @Operation(summary = "프로젝트 이미지 목록 조회", description = "특정 프로젝트의 모든 이미지 목록을 조회합니다.")
    @GetMapping("/project/{projectId}/images")
    public ResponseEntity<List<Image>> getImagesList(@PathVariable String projectId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        List<Image> images = imageService.getImagesList(projectId, page, size);
        return ResponseEntity.ok(images);
    }

    @Operation(summary = "이미지 상세 조회", description = "이미지 ID로 특정 이미지를 조회합니다.")
    @GetMapping("/images/{id}")
    public ResponseEntity<Image> getImageById(@PathVariable Long id) {
        Image image = imageService.getImageById(id);
        return ResponseEntity.ok(image);
    }

    @Operation(summary = "이미지 수정", description = "기존 이미지를 새로운 이미지로 교체합니다.")
    @PatchMapping("/images/{id}")
    public ResponseEntity<?> updateImageById(@PathVariable Long id, @RequestParam("image") MultipartFile image) {
        try {
            // 1. 원본 이미지 업로드 및 DB 저장
            String imageId = imageService.patchImage(id,image);
            
            return ResponseEntity.ok(Map.of(
                "imageId", imageId,
                "message", "Image uploaded successfully. Thumbnail is being processed.",
                "thumbnailStatus", "PROCESSING"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    @Operation(summary = "이미지 삭제", description = "특정 이미지를 삭제합니다.")
    @DeleteMapping("/images/{id}")
    public ResponseEntity<String> deleteImageById(@PathVariable Long id) {
        imageService.deleteImage(id);
        return ResponseEntity.ok("Image with ID: " + id + " deleted successfully");
    }
}
