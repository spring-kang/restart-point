package com.restartpoint.domain.guide.entity;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주차별 가이드라인
 * 각 주차에 해야 할 목표, 마일스톤, 추천 액션 등을 정의
 */
@Entity
@Table(name = "weekly_guidelines", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"template_id", "week_number"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyGuideline extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProjectTemplate template;

    // 주차 번호 (1부터 시작)
    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    // 주차 제목 (예: "아이디어 구체화", "프로토타입 개발")
    @Column(nullable = false)
    private String title;

    // 주차 설명
    @Column(length = 2000)
    private String description;

    // 이번 주 핵심 목표 (마크다운 형식)
    @Column(name = "key_objectives", length = 2000)
    private String keyObjectives;

    // 달성해야 할 마일스톤 (JSON 배열 형태로 저장)
    @Column(name = "milestones", length = 2000)
    private String milestones;

    // 추천 액션 (JSON 배열 형태로 저장)
    @Column(name = "recommended_actions", length = 2000)
    private String recommendedActions;

    // 상세 가이드 내용 (마크다운)
    @Column(name = "guide_content", length = 5000)
    private String guideContent;

    // 이번 주 집중해야 할 직무 (선택사항)
    @Enumerated(EnumType.STRING)
    @Column(name = "focus_role")
    private JobRole focusRole;

    // 체크리스트 항목 (JSON 배열 형태로 저장)
    @Column(name = "checklist_items", length = 2000)
    private String checklistItems;

    // 참고 자료 링크 (JSON 배열 형태로 저장)
    @Column(name = "reference_links", length = 1000)
    private String referenceLinks;

    @Builder
    public WeeklyGuideline(ProjectTemplate template, Integer weekNumber, String title,
                           String description, String keyObjectives, String milestones,
                           String recommendedActions, String guideContent, JobRole focusRole,
                           String checklistItems, String referenceLinks) {
        this.template = template;
        this.weekNumber = weekNumber;
        this.title = title;
        this.description = description;
        this.keyObjectives = keyObjectives;
        this.milestones = milestones;
        this.recommendedActions = recommendedActions;
        this.guideContent = guideContent;
        this.focusRole = focusRole;
        this.checklistItems = checklistItems;
        this.referenceLinks = referenceLinks;
    }

    public void update(String title, String description, String keyObjectives,
                       String milestones, String recommendedActions, String guideContent,
                       JobRole focusRole, String checklistItems, String referenceLinks) {
        this.title = title;
        this.description = description;
        this.keyObjectives = keyObjectives;
        this.milestones = milestones;
        this.recommendedActions = recommendedActions;
        this.guideContent = guideContent;
        this.focusRole = focusRole;
        this.checklistItems = checklistItems;
        this.referenceLinks = referenceLinks;
    }
}
