package markoala.fithub.demo.domain.meeting;

import markoala.fithub.demo.domain.meeting.dto.MeetingAttendeeCreateRequest;
import markoala.fithub.demo.domain.meeting.dto.MeetingLogCreateRequest;
import markoala.fithub.demo.domain.meeting.dto.MeetingLogResponse;
import markoala.fithub.demo.domain.meeting.dto.MeetingStepRelationResponse;
import markoala.fithub.demo.domain.meeting.dto.MeetingSummarizeResponse;
import markoala.fithub.demo.domain.meeting.dto.TranslateToPlanningRequest;
import markoala.fithub.demo.domain.meeting.dto.TranslateToPlanningResponse;
import markoala.fithub.demo.domain.meeting.dto.TranslateToTechnicalRequest;
import markoala.fithub.demo.domain.meeting.dto.TranslateToTechnicalResponse;
import markoala.fithub.demo.domain.meeting.dto.TranslationSearchResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class MeetingClient {

    private final RestClient restClient;

    public MeetingClient(@Qualifier("fastApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public MeetingLogResponse createMeetingLog(MeetingLogCreateRequest request) {
        return restClient.post()
                .uri("/meetings/")
                .body(request)
                .retrieve()
                .body(MeetingLogResponse.class);
    }

    public MeetingLogResponse getMeetingLog(Long meetingId) {
        return restClient.get()
                .uri("/meetings/{meetingId}", meetingId)
                .retrieve()
                .body(MeetingLogResponse.class);
    }

    public List<MeetingLogResponse> getMeetingsByProject(Long projectId) {
        return restClient.get()
                .uri("/meetings/project/{projectId}", projectId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MeetingLogResponse>>() {});
    }

    public MeetingSummarizeResponse summarizeMeeting(Long meetingId) {
        return restClient.post()
                .uri("/meetings/{meetingId}/summarize", meetingId)
                .retrieve()
                .body(MeetingSummarizeResponse.class);
    }

    public void addAttendee(Long meetingId, MeetingAttendeeCreateRequest request) {
        restClient.post()
                .uri("/meetings/{meetingId}/attendees", meetingId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public MeetingStepRelationResponse linkStepToMeeting(Long meetingId, Long stepId) {
        return restClient.post()
                .uri("/meetings/{meetingId}/steps/{stepId}", meetingId, stepId)
                .retrieve()
                .body(MeetingStepRelationResponse.class);
    }

    // ─────────────────────────────────────────────────────────────────
    // 번역 및 인텔리전스 (Translation Router 연동)
    // ─────────────────────────────────────────────────────────────────

    public TranslationSearchResponse searchMeetings(String query, int limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/meetings/search")
                        .queryParam("query", query)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(TranslationSearchResponse.class);
    }

    public TranslateToTechnicalResponse translateToTechnical(Long meetingId, TranslateToTechnicalRequest request) {
        return restClient.post()
                .uri("/meetings/{meetingId}/translate-to-technical", meetingId)
                .body(request)
                .retrieve()
                .body(TranslateToTechnicalResponse.class);
    }

    public TranslateToPlanningResponse translateToPlanning(Long meetingId, TranslateToPlanningRequest request) {
        return restClient.post()
                .uri("/meetings/{meetingId}/translate-to-planning", meetingId)
                .body(request)
                .retrieve()
                .body(TranslateToPlanningResponse.class);
    }

    public void finalizeTranslationSession(Long meetingId) {
        restClient.post()
                .uri("/meetings/{meetingId}/finalize-translation-session", meetingId)
                .retrieve()
                .toBodilessEntity();
    }

    public Object getTranslationHistory(Long meetingId) {
        return restClient.get()
                .uri("/meetings/{meetingId}/translation-history", meetingId)
                .retrieve()
                .body(Object.class);
    }

    public MeetingLogResponse updateMeetingLog(Long meetingId, MeetingLogCreateRequest request) {
        return restClient.patch()
                .uri("/meetings/{meetingId}", meetingId)
                .body(request)
                .retrieve()
                .body(MeetingLogResponse.class);
    }

    public void deleteMeetingLog(Long meetingId) {
        restClient.delete()
                .uri("/meetings/{meetingId}", meetingId)
                .retrieve()
                .toBodilessEntity();
    }
}
