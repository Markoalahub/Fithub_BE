package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 최종 v3 파이프라인 생성 응답 DTO
 */
public record PipelineV3Response(
    Long id,
    @JsonProperty("project_id") Long projectId,
    String category,    // "BE", "FE" 등
    Integer version,
    @JsonProperty("tech_stack") String techStack, // 파이프라인 전체 기술 스택
    List<PipelineStepV3Response> steps
) {}
