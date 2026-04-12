package markoala.fithub.demo.pipeline.dto;

import java.util.List;

public record MultiPipelineResponse(
        Long projectId,
        int totalCategories,
        List<PipelineResponse> pipelines
) {}
