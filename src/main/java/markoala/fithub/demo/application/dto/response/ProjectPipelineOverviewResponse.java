package markoala.fithub.demo.application.dto.response;

import java.util.List;

/**
 * 프로젝트 정보와 파이프라인 정보를 결합한 API Composition 응답 DTO
 */
public record ProjectPipelineOverviewResponse(
    Long projectId,
    String projectName,
    String projectDescription,
    List<PipelineV3Response> pipelines
) {}
