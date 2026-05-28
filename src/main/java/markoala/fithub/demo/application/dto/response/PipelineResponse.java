package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 수정된 v3 파이프라인 응답 DTO
 */
public record PipelineResponse(
        Long id,
        @JsonProperty("project_id")
        Long projectId,
        String category,
        Integer version,
        List<PipelineStepResponse> steps
) {}
