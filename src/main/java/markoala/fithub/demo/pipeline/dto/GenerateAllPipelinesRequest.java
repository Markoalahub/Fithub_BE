package markoala.fithub.demo.pipeline.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 전체 카테고리 파이프라인 생성 요청
 *
 * @param projectId Spring Project ID (필수)
 */
public record GenerateAllPipelinesRequest(
        @NotNull(message = "projectId는 필수입니다")
        @Schema(description = "Spring Project ID", example = "1")
        Long projectId
) {}
