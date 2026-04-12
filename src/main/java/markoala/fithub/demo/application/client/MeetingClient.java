package markoala.fithub.demo.application.client;

import markoala.fithub.demo.application.dto.request.MeetingAttendeeCreateRequest;
import markoala.fithub.demo.application.dto.request.MeetingLogCreateRequest;
import markoala.fithub.demo.application.dto.response.MeetingLogResponse;
import markoala.fithub.demo.application.dto.response.MeetingStepRelationResponse;
import markoala.fithub.demo.application.dto.response.MeetingSummarizeResponse;
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
