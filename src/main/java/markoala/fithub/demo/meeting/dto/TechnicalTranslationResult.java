package markoala.fithub.demo.meeting.dto;

import java.util.List;

/**
 * 기획자 -> 개발자 기술 번역 상세 결과
 */
public record TechnicalTranslationResult(
    String problemStatement,
    List<String> technicalApproach,
    List<String> techStack,
    String effortEstimate,
    List<String> dependencies
) {}
