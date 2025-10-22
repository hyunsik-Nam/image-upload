package com.first.image.upload.entity;

public enum ThumbnailStatus {
    PROCESSING,  // 썸네일 생성 중
    READY,       // 썸네일 준비 완료
    FAILED       // 썸네일 생성 실패 (3회 시도 후)
}
