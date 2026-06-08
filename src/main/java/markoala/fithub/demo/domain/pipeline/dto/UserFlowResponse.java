package markoala.fithub.demo.domain.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UserFlowResponse(
        Long id,
        @JsonProperty("project_id")
        Long projectId,
        String title,
        @JsonProperty("prd_context")
        String prdContext,
        @JsonProperty("session_status")
        String sessionStatus,
        @JsonProperty("current_turn")
        Integer currentTurn,
        List<UserFlowNodeResponse> nodes,
        List<UserFlowEdgeResponse> edges
) {}
