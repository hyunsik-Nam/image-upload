package com.first.image.upload.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.first.image.upload.entity.Image;
import com.first.image.upload.entity.ThumbnailStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    // 프로젝트별 이미지 조회
    List<Image> findByProjectId(String projectId);
    
    // 프로젝트별 이미지 페이징 조회
    Page<Image> findByProjectId(String projectId, Pageable pageable);
    
    // 썸네일 상태별 조회
    List<Image> findByThumbnailStatus(ThumbnailStatus thumbnailStatus);
    
    // 프로젝트별 + 썸네일 상태별 조회
    List<Image> findByProjectIdAndThumbnailStatus(String projectId, ThumbnailStatus thumbnailStatus);
    
    // 파일명으로 조회
    Optional<Image> findByFileName(String fileName);
    
    // 실패한 썸네일 재시도 대상 조회 (3회 미만)
    @Query("SELECT i FROM Image i WHERE i.thumbnailStatus = 'PROCESSING' AND i.retryCount < 3 AND i.createdAt < :beforeTime")
    List<Image> findRetryableImages(@Param("beforeTime") LocalDateTime beforeTime);
    
    // 프로젝트별 이미지 수 조회
    long countByProjectId(String projectId);
    
    // 특정 기간 내 생성된 이미지 조회
    @Query("SELECT i FROM Image i WHERE i.createdAt BETWEEN :startDate AND :endDate")
    List<Image> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 커서 기반 페이징 (무한 스크롤용)
    @Query("SELECT i FROM Image i WHERE i.projectId = :projectId AND i.id > :cursor ORDER BY i.id ASC")
    List<Image> findByProjectIdWithCursor(@Param("projectId") String projectId, @Param("cursor") Long cursor, Pageable pageable);
    
    // 첫 페이지 조회 (커서 기반)
    @Query("SELECT i FROM Image i WHERE i.projectId = :projectId ORDER BY i.id ASC")
    List<Image> findFirstPageByProjectId(@Param("projectId") String projectId, Pageable pageable);

    // 중복 검사용
    Optional<Image> findByProjectIdAndFileHash(String projectId, String fileHash);
}