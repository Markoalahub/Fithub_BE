package markoala.fithub.demo.domain.pipeline.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ProjectPipelineSummaryListResponse(
        @JsonProperty("project_id") Long projectId,
        List<PipelineSummaryResponse> pipelines,
        Long total
) {}
