package markoala.fithub.demo.meeting;

import markoala.fithub.demo.meeting.dto.MeetingAttendeeCreateRequest;
import markoala.fithub.demo.meeting.dto.MeetingLogCreateRequest;
import markoala.fithub.demo.meeting.dto.MeetingLogResponse;
import markoala.fithub.demo.meeting.dto.MeetingStepRelationResponse;
import markoala.fithub.demo.meeting.dto.MeetingSummarizeResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public java.util.List<MeetingLogResponse> getMeetingsByProject(Long projectId) {
        return restClient.get()
                .uri("/meetings/project/{projectId}", projectId)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<java.util.List<MeetingLogResponse>>() {});
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
