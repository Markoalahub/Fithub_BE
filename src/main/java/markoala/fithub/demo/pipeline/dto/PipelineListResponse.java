package markoala.fithub.demo.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PipelineListResponse(
        List<PipelineResponse> pipelines,
        Long total
) {}
