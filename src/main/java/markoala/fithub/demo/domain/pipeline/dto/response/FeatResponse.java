package markoala.fithub.demo.domain.pipeline.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FeatResponse(
    @JsonProperty("feat_id")
    @JsonAlias("id")
    Long featId,
    @JsonProperty("feat_title")
    @JsonAlias("step_task_description")
    String featTitle,
    @JsonProperty("feat_details")
    @JsonAlias("step_details")
    List<String> featDetails,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(hidden = true)
    Integer priority
) {}
