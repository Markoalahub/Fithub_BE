package markoala.fithub.demo.meeting.dto;

import java.util.List;

/**
 * 개발자 -> 기획자 비즈니스 번역 상세 결과
 */
public record PlanningTranslationResult(
    String simpleExplanation,
    String analogy,
    String impact,
    String timeline,
    String whyNeeded
) {}
