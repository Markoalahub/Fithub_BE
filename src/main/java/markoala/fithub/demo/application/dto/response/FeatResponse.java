package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FeatResponse(
    @JsonProperty("feat_id") Long featId,
    @JsonProperty("feat_title") String featTitle,
    @JsonProperty("feat_details") List<String> featDetails,
    Integer priority
) {}
