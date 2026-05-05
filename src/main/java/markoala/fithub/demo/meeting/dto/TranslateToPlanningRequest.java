package markoala.fithub.demo.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TranslateToPlanningRequest(
    @JsonProperty("developer_statement") String developerStatement,
    String context
) {}
