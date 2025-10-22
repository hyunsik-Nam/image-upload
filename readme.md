# Image Upload Service

Spring Boot 기반의 이미지 업로드 및 관리 서비스입니다. MinIO를 활용한 객체 스토리지와 비동기 썸네일 생성 기능을 제공합니다.

### 구현 기능

- 이미지 업로드
- 이미지 목록조회
- 이미지 상세조회
- 등록이미지 수정
- 등록이미지 삭제

### 설치 및 실행

## 1. 사전 요구사항

# Java 17 이상

java -version

# Docker (MinIO 실행용)

docker --version

# Maven

mvn --version

## 2. MinIO 서버 실행

# MinIO 컨테이너 실행

docker run -d \
 --name minio \
 -p 9000:9000 \
 -p 9001:9001 \
 -e MINIO_ROOT_USER=minioadmin \
 -e MINIO_ROOT_PASSWORD=minioadmin \
 minio/minio server /data --console-address ":9001"

# MinIO 웹 콘솔 접속: http://localhost:9001

# 로그인: minioadmin / minioadmin

## 3. 애플리케이션 실행

# 프로젝트 클론

git clone https://github.com/yourusername/image-upload-service.git
cd image-upload-service

# 의존성 설치 및 실행

mvn clean install
mvn spring-boot:run

# 애플리케이션 접속: http://localhost:8080

## 4. 개발 환경 확인

# H2 데이터베이스 콘솔

http://localhost:8080/h2-console

# Swagger API 문서

http://localhost:8080/swagger-ui/index.html

### API 문서

## 📤 이미지 업로드

```http
POST /project/{projectId}/images
Content-Type: multipart/form-data

Parameters:
- projectId (path): 프로젝트 ID
- image (form-data): 업로드할 이미지 파일
```

## 이미지 목록 조회

```http
GET /project/{projectId}/images?page=0&size=10

Parameters:
- projectId (path): 프로젝트 ID
- page (query): 페이지 번호 (기본값: 0)
- size (query): 페이지 크기 (기본값: 10)
```

## 이미지 상세 조회

```http
GET /images/{id}

Parameters:
- id (path): 이미지 ID
```

## 이미지 수정

```http
PATCH /images/{id}
Content-Type: multipart/form-data

Parameters:
- id (path): 이미지 ID
- image (form-data): 새로운 이미지 파일
```

## 이미지 삭제

```http
DELETE /images/{id}

Parameters:
- id (path): 이미지 ID
```

### 응답 예시

## 성공 응답

```json
{
  "imageId": "projects/test-project/abc123.jpg",
  "message": "Image uploaded successfully. Thumbnail is being processed.",
  "thumbnailStatus": "PROCESSING"
}
```

#### 오류 응답

```json
{
  "error": "Upload failed: File size exceeds maximum limit"
}
```
