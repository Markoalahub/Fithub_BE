package markoala.fithub.demo.domain.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PipelineListResponse(
        List<PipelineResponse> pipelines,
        Long total
) {}
