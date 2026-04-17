package com.restartpoint.infra.ai;

import com.restartpoint.domain.project.entity.Checkpoint;
import com.restartpoint.domain.project.entity.MemberProgress;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.repository.CheckpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiProjectCoachService {

    private final GroqService groqService;
    private final CheckpointRepository checkpointRepository;

    private static final String SYSTEM_PROMPT = """
        당신은 IT 취업을 준비하는 사람들의 프로젝트 진행을 돕는 AI 프로젝트 코치입니다.
        팀의 체크포인트 데이터를 분석하여 구체적이고 실행 가능한 피드백을 제공해야 합니다.

        피드백 제공 시 다음 원칙을 따르세요:
        1. 문제 정의의 명확성을 점검하세요
        2. 사용자 가치 제안이 명확한지 확인하세요
        3. 일정 지연 리스크가 있는지 감지하세요
        4. 역할 간 업무 불균형이 있는지 확인하세요
        5. 누락된 산출물이 있는지 안내하세요
        6. 직무별로 구체적인 다음 액션을 제안하세요

        피드백은 한국어로 작성하고, 다음 형식으로 구조화하세요:

        ## 전체 진행 상황 분석
        [팀 전체의 진행 상황에 대한 종합적인 분석]

        ## 잘하고 있는 점
        - [긍정적인 점 1]
        - [긍정적인 점 2]

        ## 개선이 필요한 점
        - [개선점 1]
        - [개선점 2]

        ## 리스크 감지
        - [잠재적 리스크와 대응 방안]

        ## 역할별 액션 아이템
        - **기획**: [구체적인 액션]
        - **UXUI**: [구체적인 액션]
        - **프론트엔드**: [구체적인 액션]
        - **백엔드**: [구체적인 액션]

        ## 다음 주 추천 목표
        [다음 체크포인트까지 달성해야 할 핵심 목표]
        """;

    /**
     * 체크포인트 데이터를 기반으로 AI 피드백을 생성합니다.
     */
    public String generateFeedback(Checkpoint checkpoint) {
        Project project = checkpoint.getProject();
        String userMessage = buildUserMessage(project, checkpoint);

        log.info("AI 프로젝트 코칭 피드백 생성 시작 - 프로젝트: {}, 체크포인트: {}주차",
                project.getName(), checkpoint.getWeekNumber());

        String feedback = groqService.chat(SYSTEM_PROMPT, userMessage);

        if (feedback == null) {
            log.warn("AI 피드백 생성 실패 - 프로젝트: {}", project.getName());
            return "AI 피드백을 생성하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }

        log.info("AI 프로젝트 코칭 피드백 생성 완료 - 프로젝트: {}", project.getName());
        return feedback;
    }

    /**
     * 비동기로 AI 피드백을 생성합니다.
     */
    @Async
    public CompletableFuture<String> generateFeedbackAsync(Checkpoint checkpoint) {
        String feedback = generateFeedback(checkpoint);
        return CompletableFuture.completedFuture(feedback);
    }

    /**
     * 비동기로 AI 피드백을 생성하고 체크포인트에 저장합니다.
     * 체크포인트 저장/수정 후 호출되어 별도 트랜잭션에서 처리됩니다.
     */
    @Async
    @Transactional
    public void generateAndSaveFeedbackAsync(Long checkpointId) {
        log.info("비동기 AI 피드백 생성 시작 - 체크포인트 ID: {}", checkpointId);

        try {
            Checkpoint checkpoint = checkpointRepository.findByIdWithProject(checkpointId)
                    .orElse(null);

            if (checkpoint == null) {
                log.warn("체크포인트를 찾을 수 없음 - ID: {}", checkpointId);
                return;
            }

            String feedback = generateFeedback(checkpoint);

            if (feedback != null && !feedback.contains("오류가 발생했습니다")) {
                checkpoint.setAiFeedback(feedback);
                checkpointRepository.save(checkpoint);
                log.info("비동기 AI 피드백 저장 완료 - 체크포인트 ID: {}", checkpointId);
            } else {
                checkpoint.setAiFeedback("AI 피드백 생성 중 오류가 발생했습니다. 나중에 다시 시도해주세요.");
                checkpointRepository.save(checkpoint);
                log.warn("AI 피드백 생성 실패 - 체크포인트 ID: {}", checkpointId);
            }
        } catch (Exception e) {
            log.error("비동기 AI 피드백 생성 실패 - 체크포인트 ID: {}, 오류: {}", checkpointId, e.getMessage(), e);
        }
    }

    private String buildUserMessage(Project project, Checkpoint checkpoint) {
        StringBuilder sb = new StringBuilder();

        // 프로젝트 정보
        sb.append("## 프로젝트 정보\n");
        sb.append("- 프로젝트명: ").append(project.getName()).append("\n");
        sb.append("- 문제 정의: ").append(nullSafe(project.getProblemDefinition())).append("\n");
        sb.append("- 타깃 사용자: ").append(nullSafe(project.getTargetUsers())).append("\n");
        sb.append("- 핵심 솔루션: ").append(nullSafe(project.getSolution())).append("\n");
        sb.append("- AI 활용 방식: ").append(nullSafe(project.getAiUsage())).append("\n");
        sb.append("\n");

        // 체크포인트 정보
        sb.append("## ").append(checkpoint.getWeekNumber()).append("주차 체크포인트\n");
        sb.append("- 이번 주 목표: ").append(nullSafe(checkpoint.getWeeklyGoal())).append("\n");
        sb.append("- 진행 상황 요약: ").append(nullSafe(checkpoint.getProgressSummary())).append("\n");
        sb.append("- 막힘 요소: ").append(nullSafe(checkpoint.getBlockers())).append("\n");
        sb.append("- 다음 주 계획: ").append(nullSafe(checkpoint.getNextWeekPlan())).append("\n");
        sb.append("\n");

        // 역할별 진행 상황
        List<MemberProgress> progresses = checkpoint.getMemberProgresses();
        if (progresses != null && !progresses.isEmpty()) {
            sb.append("## 역할별 진행 상황\n");
            for (MemberProgress progress : progresses) {
                sb.append("### ").append(progress.getJobRole().name()).append("\n");
                sb.append("- 완료한 작업: ").append(nullSafe(progress.getCompletedTasks())).append("\n");
                sb.append("- 진행 중인 작업: ").append(nullSafe(progress.getInProgressTasks())).append("\n");
                sb.append("- 개인 막힘 요소: ").append(nullSafe(progress.getPersonalBlockers())).append("\n");
                sb.append("- 기여도: ").append(progress.getContributionPercentage()).append("%\n");
                sb.append("\n");
            }
        }

        // 이전 체크포인트 요약 (있는 경우)
        List<Checkpoint> previousCheckpoints = project.getCheckpoints().stream()
                .filter(cp -> cp.getWeekNumber() < checkpoint.getWeekNumber())
                .toList();

        if (!previousCheckpoints.isEmpty()) {
            sb.append("## 이전 체크포인트 요약\n");
            for (Checkpoint prev : previousCheckpoints) {
                sb.append("- ").append(prev.getWeekNumber()).append("주차: ")
                        .append(nullSafe(prev.getProgressSummary())).append("\n");
            }
        }

        return sb.toString();
    }

    private String nullSafe(String value) {
        return value != null ? value : "미입력";
    }
}
