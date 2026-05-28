package markoala.fithub.demo.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record TranslationSearchResponse(
    String query,
    @JsonProperty("total_results") int totalResults,
    List<TranslationSearchResult> results,
    @JsonProperty("search_time_ms") double searchTimeMs
) {}

record TranslationSearchResult(
    @JsonProperty("meeting_id") Long meetingId,
    String summary,
    @JsonProperty("session_date") LocalDateTime sessionDate,
    @JsonProperty("relevance_score") double relevanceScore,
    @JsonProperty("conversation_type") String conversationType
) {}
