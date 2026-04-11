package markoala.fithub.demo.application.dto.response;

import java.util.List;

public record MultiPipelineResponse(
        Long projectId,
        int totalCategories,
        List<PipelineResponse> pipelines
) {}
