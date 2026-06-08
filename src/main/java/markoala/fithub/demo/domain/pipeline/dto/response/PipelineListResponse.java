package markoala.fithub.demo.domain.pipeline.dto.response;

import java.util.List;

/**
 * 파이프라인 목록 응답 DTO (V3 구조 사용)
 */
public record PipelineListResponse(
        List<PipelineV3Response> pipelines,
        Long total
) {}
