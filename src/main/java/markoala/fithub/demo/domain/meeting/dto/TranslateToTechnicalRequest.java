package markoala.fithub.demo.domain.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TranslateToTechnicalRequest(
    @JsonProperty("original_statement") String originalStatement,
    String context
) {}
