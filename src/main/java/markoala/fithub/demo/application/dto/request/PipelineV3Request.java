package markoala.fithub.demo.application.dto.request;

import org.springframework.web.multipart.MultipartFile;

/**
 * v4 통합형 파이프라인 생성 요청 DTO
 */
public record PipelineV3Request(
    Long projectId,
    String requirements,
    String category,    // "ALL", "BE", "FE", "DB", "INFRA", "AI"
    String techStack,   // 프로젝트 전체 기술 스택
    MultipartFile file  // 기획서 파일 (필드명: file)
) {}
