package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PipelineListResponse(
        List<PipelineResponse> pipelines,
        Long total
) {}
