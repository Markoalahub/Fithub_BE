package markoala.fithub.demo.domain.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserFlowEdgeResponse(
        Long id,
        @JsonProperty("user_flow_id")
        Long userFlowId,
        @JsonProperty("from_node_id")
        Long fromNodeId,
        @JsonProperty("to_node_id")
        Long toNodeId,
        String label
) {}
