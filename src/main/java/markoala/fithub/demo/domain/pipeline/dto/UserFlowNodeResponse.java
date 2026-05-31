package markoala.fithub.demo.domain.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserFlowNodeResponse(
        Long id,
        @JsonProperty("user_flow_id")
        Long userFlowId,
        String name,
        String description,
        @JsonProperty("node_type")
        String nodeType,
        @JsonProperty("wireframe_ascii")
        String wireframeAscii,
        @JsonProperty("sequence_order")
        Integer sequenceOrder
) {}
