package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * v4 통합형 파이프라인 스텝 응답 DTO
 */
public record PipelineStepV3Response(
    Long id,
    @JsonProperty("step_task_description") String stepTaskDescription, // 요약 제목
    @JsonProperty("step_details") List<String> stepDetails,            // 상세 작업 리스트
    
    // v4 신규 필드
    String category,                                  // "DB", "INFRA", "BE", "AI", "FE"
    @JsonProperty("priority") Integer priority,       // 1(핵심), 2(부가)
    @JsonProperty("tech_stack") List<String> techStack, // 이슈 라벨용 리스트
    @JsonProperty("depends_on") List<Integer> dependsOn, // 선행 sequence 리스트
    
    @JsonProperty("step_sequence_number") Integer stepSequenceNumber,
    @JsonProperty("deadline_date") LocalDate deadlineDate,
    @JsonProperty("deadline_time") LocalTime deadlineTime
) {}
