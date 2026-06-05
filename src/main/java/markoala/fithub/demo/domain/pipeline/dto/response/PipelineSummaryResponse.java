package markoala.fithub.demo.domain.pipeline.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PipelineSummaryResponse(
        @JsonProperty("pipe_id") Long pipeId,
        @JsonProperty("pipeline_name") String pipelineName,
        String category
) {}
