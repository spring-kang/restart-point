package com.restartpoint.domain.guide.entity;

import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 시즌별 프로젝트 가이드 템플릿
 * 시즌의 프로젝트 기간에 맞춰 주차별 가이드를 제공
 */
@Entity
@Table(name = "project_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // 총 주차 수 (시즌 기간에서 자동 계산 가능하지만, 명시적으로 저장)
    @Column(name = "total_weeks", nullable = false)
    private Integer totalWeeks;

    // 활성화 여부
    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC")
    private List<WeeklyGuideline> guidelines = new ArrayList<>();

    @Builder
    public ProjectTemplate(Season season, String title, String description, Integer totalWeeks) {
        this.season = season;
        this.title = title;
        this.description = description;
        this.totalWeeks = totalWeeks;
        this.active = true;
    }

    public void update(String title, String description, Integer totalWeeks) {
        this.title = title;
        this.description = description;
        this.totalWeeks = totalWeeks;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void addGuideline(WeeklyGuideline guideline) {
        this.guidelines.add(guideline);
    }

    public void removeGuideline(WeeklyGuideline guideline) {
        this.guidelines.remove(guideline);
    }
}
