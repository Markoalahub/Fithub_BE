package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FeatResponse(
    @JsonAlias({"feat_id", "id"}) Long featId,
    @JsonAlias({"feat_title", "step_task_description"}) String featTitle,
    @JsonAlias({"feat_details", "step_details"}) List<String> featDetails,
    Integer priority
) {}
