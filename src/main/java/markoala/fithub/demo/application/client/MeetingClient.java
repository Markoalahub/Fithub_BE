package markoala.fithub.demo.application.client;

import markoala.fithub.demo.application.dto.request.MeetingAttendeeCreateRequest;
import markoala.fithub.demo.application.dto.request.MeetingLogCreateRequest;
import markoala.fithub.demo.application.dto.response.MeetingLogResponse;
import markoala.fithub.demo.application.dto.response.MeetingStepRelationResponse;
import markoala.fithub.demo.application.dto.response.MeetingSummarizeResponse;
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
}
