package com.restartpoint.infra.ai;

import com.restartpoint.domain.project.entity.Checkpoint;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.team.entity.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiProjectCoachServiceTest {

    @Mock
    private GroqService groqService;

    @InjectMocks
    private AiProjectCoachService aiProjectCoachService;

    @Test
    @DisplayName("체크포인트 데이터를 기반으로 AI 피드백을 생성한다")
    void generateFeedback_Success() {
        // given
        Team team = Team.builder()
                .name("테스트팀")
                .build();

        Project project = Project.builder()
                .team(team)
                .name("테스트 프로젝트")
                .problemDefinition("사용자들이 팀을 구하기 어려운 문제")
                .targetUsers("부트캠프 수료생")
                .solution("AI 기반 팀 매칭 서비스")
                .aiUsage("팀 추천 및 프로젝트 코칭")
                .build();

        Checkpoint checkpoint = Checkpoint.builder()
                .project(project)
                .weekNumber(1)
                .weeklyGoal("MVP 기획 완료")
                .progressSummary("문제 정의 및 사용자 조사 완료")
                .blockers("기술 스택 선정 고민 중")
                .nextWeekPlan("와이어프레임 작성")
                .build();

        String expectedFeedback = """
                ## 전체 진행 상황 분석
                1주차 진행 상황이 전반적으로 양호합니다.

                ## 잘하고 있는 점
                - 문제 정의가 명확합니다
                - 사용자 조사를 먼저 진행한 점이 좋습니다

                ## 개선이 필요한 점
                - 기술 스택 선정을 빨리 결정해야 합니다
                """;

        given(groqService.chat(anyString(), anyString())).willReturn(expectedFeedback);

        // when
        String feedback = aiProjectCoachService.generateFeedback(checkpoint);

        // then
        assertThat(feedback).isEqualTo(expectedFeedback);
        verify(groqService).chat(anyString(), anyString());
    }

    @Test
    @DisplayName("AI API 호출 실패 시 오류 메시지를 반환한다")
    void generateFeedback_WhenApiFails_ReturnsErrorMessage() {
        // given
        Team team = Team.builder()
                .name("테스트팀")
                .build();

        Project project = Project.builder()
                .team(team)
                .name("테스트 프로젝트")
                .build();

        Checkpoint checkpoint = Checkpoint.builder()
                .project(project)
                .weekNumber(1)
                .weeklyGoal("테스트 목표")
                .build();

        given(groqService.chat(anyString(), anyString())).willReturn(null);

        // when
        String feedback = aiProjectCoachService.generateFeedback(checkpoint);

        // then
        assertThat(feedback).contains("AI 피드백을 생성하는 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("프로젝트 정보가 누락되어도 피드백을 생성한다")
    void generateFeedback_WithMissingProjectInfo_StillGeneratesFeedback() {
        // given
        Team team = Team.builder()
                .name("테스트팀")
                .build();

        Project project = Project.builder()
                .team(team)
                .name("테스트 프로젝트")
                .build();  // 다른 필드는 null

        Checkpoint checkpoint = Checkpoint.builder()
                .project(project)
                .weekNumber(1)
                .build();  // 최소한의 정보만

        String expectedFeedback = "최소 정보로 생성된 피드백";
        given(groqService.chat(anyString(), anyString())).willReturn(expectedFeedback);

        // when
        String feedback = aiProjectCoachService.generateFeedback(checkpoint);

        // then
        assertThat(feedback).isEqualTo(expectedFeedback);
    }
}
