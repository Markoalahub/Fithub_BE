package markoala.fithub.demo.domain.pipeline.dto;

import java.util.List;

public record MultiPipelineResponse(
        Long projectId,
        int totalCategories,
        List<PipelineResponse> pipelines
) {}
