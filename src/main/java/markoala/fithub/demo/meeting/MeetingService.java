package markoala.fithub.demo.meeting;

import markoala.fithub.demo.issue.Issue;
import markoala.fithub.demo.issue.IssueRepository;
import markoala.fithub.demo.meeting.dto.MeetingLogCreateRequest;
import markoala.fithub.demo.meeting.dto.MeetingLogResponse;
import markoala.fithub.demo.meeting.dto.MeetingStepRelationResponse;
import markoala.fithub.demo.meeting.dto.MeetingSummarizeResponse;
import markoala.fithub.demo.pipeline.PipelineClient;
import markoala.fithub.demo.pipeline.dto.PipelineStepCreateRequest;
import markoala.fithub.demo.pipeline.dto.PipelineStepResponse;
import markoala.fithub.demo.project.ProjectMemberRepository;
import markoala.fithub.demo.project.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    private final MeetingClient meetingClient;
    private final PipelineClient pipelineClient;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public MeetingService(
            MeetingClient meetingClient,
            PipelineClient pipelineClient,
            IssueRepository issueRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.meetingClient = meetingClient;
        this.pipelineClient = pipelineClient;
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    /**
     * 회의록 생성
     * - 프로젝트 존재 여부 확인
     * - 제안자(proposerId) / 수신자(recipientId) 모두 해당 프로젝트 멤버인지 검증
     * - FastAPI에 회의록 생성 요청 (attendeeUserIds 포함)
     */
    public MeetingLogResponse createMeeting(Long projectId, String content,
                                             Long proposerId, Long recipientId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        validateProjectMember(projectId, proposerId, "proposer");
        validateProjectMember(projectId, recipientId, "recipient");

        List<Long> attendeeIds = List.of(proposerId, recipientId);
        MeetingLogCreateRequest request = new MeetingLogCreateRequest(projectId, content, attendeeIds);

        MeetingLogResponse response = meetingClient.createMeetingLog(request);
        log.info("[Meeting Service] Meeting created: id={}, project={}", response.id(), projectId);
        return response;
    }

    /**
     * 단일 회의록 조회 (양방향 통신처럼 FastAPI에서 최신 상태 pull)
     */
    @Transactional(readOnly = true)
    public MeetingLogResponse getMeeting(Long meetingId) {
        log.info("[Meeting Service] Fetching meeting {}", meetingId);
        return meetingClient.getMeetingLog(meetingId);
    }

    /**
     * 프로젝트의 회의록 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MeetingLogResponse> getMeetingsByProject(Long projectId) {
        log.info("[Meeting Service] Fetching meetings for project {}", projectId);
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        return meetingClient.getMeetingsByProject(projectId);
    }

    /**
     * 회의록 AI 요약 및 파이프라인 스텝 제안 도출
     */
    public MeetingSummarizeResponse summarizeMeeting(Long meetingId) {
        log.info("[Meeting Service] Summarizing meeting {}", meetingId);
        MeetingSummarizeResponse response = meetingClient.summarizeMeeting(meetingId);
        log.info("[Meeting Service] Meeting {} summarized, {} derived steps",
                meetingId, response.derivedSteps().size());
        return response;
    }

    /**
     * 회의록에서 파이프라인 스텝으로 컨펌 추가
     * - 기획자 또는 개발자가 컨펌 시 호출
     * - FastAPI 파이프라인에 스텝 추가 + Spring DB에 Issue로 저장
     * - 회의록 ↔ 스텝 연결 관계도 생성
     */
    public PipelineStepResponse confirmStepFromMeeting(Long meetingId, Long pipelineId,
                                                        String title, String description) {
        log.info("[Meeting Service] Confirming step from meeting {} to pipeline {}: {}",
                meetingId, pipelineId, title);

        // FastAPI: 파이프라인에 스텝 추가 (origin = "meeting_derived")
        PipelineStepCreateRequest stepRequest = new PipelineStepCreateRequest(
                title, description, false, "meeting_derived"
        );
        PipelineStepResponse stepResponse = pipelineClient.addPipelineStep(pipelineId, stepRequest);

        // Spring DB: Issue로도 저장
        Issue issue = Issue.createIssue(null, null, stepResponse.title(), stepResponse.description(), "PENDING");
        issue.setPipelineStepId(stepResponse.id().intValue());
        issueRepository.save(issue);

        // FastAPI: 회의록 ↔ 스텝 연결
        MeetingStepRelationResponse relation = meetingClient.linkStepToMeeting(meetingId, stepResponse.id());
        log.info("[Meeting Service] Step {} linked to meeting {} (relation id={})",
                stepResponse.id(), meetingId, relation.id());

        return stepResponse;
    }

    private void validateProjectMember(Long projectId, Long userId, String role) {
        projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("User %d (%s) is not a member of project %d", userId, role, projectId)
                ));
    }
}
