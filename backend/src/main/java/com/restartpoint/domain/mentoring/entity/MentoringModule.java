package com.restartpoint.domain.mentoring.entity;

import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멘토링 모듈 (주차별 학습 내용)
 * 각 직무의 주차별 멘토링 커리큘럼을 정의
 */
@Entity
@Table(name = "mentoring_modules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"mentoring_id", "week_number"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentoringModule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentoring_id", nullable = false)
    private JobRoleMentoring mentoring;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // 학습 내용 (마크다운)
    @Column(name = "learning_content", length = 5000)
    private String learningContent;

    // 핵심 포인트 (JSON 배열)
    @Column(name = "key_points", length = 2000)
    private String keyPoints;

    // 흔한 실수 사항 (JSON 배열)
    @Column(name = "common_mistakes", length = 2000)
    private String commonMistakes;

    // 실습 과제 (JSON 배열)
    @Column(name = "practice_tasks", length = 2000)
    private String practiceTasks;

    // 참고 자료 (JSON 배열)
    @Column(name = "reference_materials", length = 2000)
    private String referenceMaterials;

    // 예상 학습 시간 (분)
    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Builder
    public MentoringModule(JobRoleMentoring mentoring, Integer weekNumber, String title,
                           String description, String learningContent, String keyPoints,
                           String commonMistakes, String practiceTasks, String referenceMaterials,
                           Integer estimatedMinutes) {
        this.mentoring = mentoring;
        this.weekNumber = weekNumber;
        this.title = title;
        this.description = description;
        this.learningContent = learningContent;
        this.keyPoints = keyPoints;
        this.commonMistakes = commonMistakes;
        this.practiceTasks = practiceTasks;
        this.referenceMaterials = referenceMaterials;
        this.estimatedMinutes = estimatedMinutes;
    }

    public void update(String title, String description, String learningContent,
                       String keyPoints, String commonMistakes, String practiceTasks,
                       String referenceMaterials, Integer estimatedMinutes) {
        this.title = title;
        this.description = description;
        this.learningContent = learningContent;
        this.keyPoints = keyPoints;
        this.commonMistakes = commonMistakes;
        this.practiceTasks = practiceTasks;
        this.referenceMaterials = referenceMaterials;
        this.estimatedMinutes = estimatedMinutes;
    }
}
