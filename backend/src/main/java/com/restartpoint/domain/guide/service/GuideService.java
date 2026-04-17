package com.restartpoint.domain.guide.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.guide.dto.*;
import com.restartpoint.domain.guide.entity.GuidelineCompletion;
import com.restartpoint.domain.guide.entity.ProjectTemplate;
import com.restartpoint.domain.guide.entity.WeeklyGuideline;
import com.restartpoint.domain.guide.repository.GuidelineCompletionRepository;
import com.restartpoint.domain.guide.repository.ProjectTemplateRepository;
import com.restartpoint.domain.guide.repository.WeeklyGuidelineRepository;
import com.restartpoint.domain.project.entity.Checkpoint;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideService {

    private final ProjectTemplateRepository templateRepository;
    private final WeeklyGuidelineRepository guidelineRepository;
    private final GuidelineCompletionRepository completionRepository;
    private final SeasonRepository seasonRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ========== ProjectTemplate 관련 ==========

    @Transactional
    public ProjectTemplateResponse createTemplate(Long seasonId, ProjectTemplateRequest request) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));

        ProjectTemplate template = ProjectTemplate.builder()
                .season(season)
                .title(request.getTitle())
                .description(request.getDescription())
                .totalWeeks(request.getTotalWeeks())
                .build();

        ProjectTemplate saved = templateRepository.save(template);
        return ProjectTemplateResponse.from(saved);
    }

    @Transactional
    public ProjectTemplateResponse createTemplateFromSeason(Long seasonId, String title, String description) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));

        // 시즌 프로젝트 기간에서 주차 수 계산
        int totalWeeks = calculateWeeks(season.getProjectStartAt(), season.getProjectEndAt());

        ProjectTemplate template = ProjectTemplate.builder()
                .season(season)
                .title(title != null ? title : season.getTitle() + " 가이드")
                .description(description)
                .totalWeeks(totalWeeks)
                .build();

        ProjectTemplate saved = templateRepository.save(template);
        return ProjectTemplateResponse.from(saved);
    }

    public ProjectTemplateResponse getTemplate(Long templateId) {
        ProjectTemplate template = templateRepository.findByIdWithGuidelines(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
        return ProjectTemplateResponse.fromWithGuidelines(template);
    }

    public List<ProjectTemplateResponse> getTemplatesBySeasonId(Long seasonId) {
        return templateRepository.findBySeasonId(seasonId).stream()
                .map(ProjectTemplateResponse::from)
                .collect(Collectors.toList());
    }

    public ProjectTemplateResponse getActiveTemplate(Long seasonId) {
        ProjectTemplate template = templateRepository.findActiveBySeasonIdWithGuidelines(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
        return ProjectTemplateResponse.fromWithGuidelines(template);
    }

    @Transactional
    public ProjectTemplateResponse updateTemplate(Long templateId, ProjectTemplateRequest request) {
        ProjectTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

        template.update(request.getTitle(), request.getDescription(), request.getTotalWeeks());
        return ProjectTemplateResponse.from(template);
    }

    @Transactional
    public void activateTemplate(Long templateId) {
        ProjectTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

        // 같은 시즌의 다른 템플릿 비활성화
        templateRepository.findBySeasonIdAndActiveTrue(template.getSeason().getId())
                .forEach(t -> {
                    if (!t.getId().equals(templateId)) {
                        t.deactivate();
                    }
                });

        template.activate();
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        ProjectTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
        templateRepository.delete(template);
    }

    // ========== WeeklyGuideline 관련 ==========

    @Transactional
    public WeeklyGuidelineResponse createGuideline(Long templateId, WeeklyGuidelineRequest request) {
        ProjectTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

        // 주차 번호 검증
        if (request.getWeekNumber() > template.getTotalWeeks()) {
            throw new BusinessException(ErrorCode.INVALID_WEEK_NUMBER);
        }

        // 중복 주차 검증
        if (guidelineRepository.existsByTemplateIdAndWeekNumber(templateId, request.getWeekNumber())) {
            throw new BusinessException(ErrorCode.DUPLICATE_WEEK_NUMBER);
        }

        WeeklyGuideline guideline = WeeklyGuideline.builder()
                .template(template)
                .weekNumber(request.getWeekNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .keyObjectives(request.getKeyObjectives())
                .milestones(toJson(request.getMilestones()))
                .recommendedActions(toJson(request.getRecommendedActions()))
                .guideContent(request.getGuideContent())
                .focusRole(request.getFocusRole())
                .checklistItems(toJson(request.getChecklistItems()))
                .referenceLinks(toJson(request.getReferenceLinks()))
                .build();

        template.addGuideline(guideline);
        WeeklyGuideline saved = guidelineRepository.save(guideline);
        return WeeklyGuidelineResponse.from(saved);
    }

    public WeeklyGuidelineResponse getGuideline(Long guidelineId) {
        WeeklyGuideline guideline = guidelineRepository.findById(guidelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDELINE_NOT_FOUND));
        return WeeklyGuidelineResponse.from(guideline);
    }

    public List<WeeklyGuidelineResponse> getGuidelinesByTemplateId(Long templateId) {
        return guidelineRepository.findByTemplateIdOrderByWeekNumberAsc(templateId).stream()
                .map(WeeklyGuidelineResponse::from)
                .collect(Collectors.toList());
    }

    public WeeklyGuidelineResponse getGuidelineBySeasonAndWeek(Long seasonId, Integer weekNumber) {
        WeeklyGuideline guideline = guidelineRepository.findBySeasonIdAndWeekNumber(seasonId, weekNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDELINE_NOT_FOUND));
        return WeeklyGuidelineResponse.from(guideline);
    }

    @Transactional
    public WeeklyGuidelineResponse updateGuideline(Long guidelineId, WeeklyGuidelineRequest request) {
        WeeklyGuideline guideline = guidelineRepository.findById(guidelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDELINE_NOT_FOUND));

        guideline.update(
                request.getTitle(),
                request.getDescription(),
                request.getKeyObjectives(),
                toJson(request.getMilestones()),
                toJson(request.getRecommendedActions()),
                request.getGuideContent(),
                request.getFocusRole(),
                toJson(request.getChecklistItems()),
                toJson(request.getReferenceLinks())
        );

        return WeeklyGuidelineResponse.from(guideline);
    }

    @Transactional
    public void deleteGuideline(Long guidelineId) {
        WeeklyGuideline guideline = guidelineRepository.findById(guidelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDELINE_NOT_FOUND));
        guidelineRepository.delete(guideline);
    }

    // ========== GuidelineCompletion 관련 ==========

    @Transactional
    public GuidelineCompletionResponse markGuidelineComplete(Long userId, Long guidelineId,
                                                              GuidelineCompletionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        WeeklyGuideline guideline = guidelineRepository.findById(guidelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDELINE_NOT_FOUND));

        GuidelineCompletion completion = completionRepository.findByUserIdAndGuidelineId(userId, guidelineId)
                .orElseGet(() -> {
                    GuidelineCompletion newCompletion = GuidelineCompletion.builder()
                            .user(user)
                            .guideline(guideline)
                            .build();
                    return completionRepository.save(newCompletion);
                });

        completion.markCompleted(request.getCompletionNotes());

        if (request.getCompletedChecklistIndexes() != null) {
            completion.updateCompletedChecklist(toJson(request.getCompletedChecklistIndexes()));
        }

        return GuidelineCompletionResponse.from(completion);
    }

    @Transactional
    public GuidelineCompletionResponse updateChecklistProgress(Long userId, Long guidelineId,
                                                                List<Integer> completedIndexes) {
        GuidelineCompletion completion = completionRepository.findByUserIdAndGuidelineId(userId, guidelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPLETION_NOT_FOUND));

        completion.updateCompletedChecklist(toJson(completedIndexes));
        return GuidelineCompletionResponse.from(completion);
    }

    @Transactional
    public void markGuidelineIncomplete(Long userId, Long guidelineId) {
        GuidelineCompletion completion = completionRepository.findByUserIdAndGuidelineId(userId, guidelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPLETION_NOT_FOUND));
        completion.markIncomplete();
    }

    @Transactional
    public GuidelineCompletionResponse linkCheckpointToGuideline(Long checkpointId, Long guidelineId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        WeeklyGuideline guideline = guidelineRepository.findById(guidelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDELINE_NOT_FOUND));

        GuidelineCompletion completion = completionRepository.findByUserIdAndGuidelineId(userId, guidelineId)
                .orElseGet(() -> {
                    GuidelineCompletion newCompletion = GuidelineCompletion.builder()
                            .user(user)
                            .guideline(guideline)
                            .build();
                    return completionRepository.save(newCompletion);
                });

        // 체크포인트 연결은 별도 서비스에서 처리 필요
        // completion.linkCheckpoint(checkpoint);

        return GuidelineCompletionResponse.from(completion);
    }

    public List<GuidelineCompletionResponse> getUserCompletions(Long userId, Long seasonId) {
        return completionRepository.findByUserIdAndSeasonId(userId, seasonId).stream()
                .map(GuidelineCompletionResponse::from)
                .collect(Collectors.toList());
    }

    public GuidelineProgressResponse getUserProgress(Long userId, Long seasonId) {
        ProjectTemplate template = templateRepository.findActiveBySeasonIdWithGuidelines(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

        List<GuidelineCompletion> completions = completionRepository
                .findByUserIdAndTemplateId(userId, template.getId());

        Map<Long, GuidelineCompletion> completionMap = completions.stream()
                .collect(Collectors.toMap(c -> c.getGuideline().getId(), c -> c));

        List<GuidelineProgressResponse.WeekProgress> weeklyProgress = new ArrayList<>();
        int completedWeeks = 0;

        for (WeeklyGuideline guideline : template.getGuidelines()) {
            GuidelineCompletion completion = completionMap.get(guideline.getId());
            boolean isCompleted = completion != null && completion.getCompleted();

            if (isCompleted) {
                completedWeeks++;
            }

            int totalChecklist = countChecklistItems(guideline.getChecklistItems());
            int completedChecklist = completion != null ?
                    countCompletedChecklist(completion.getCompletedChecklist()) : 0;

            weeklyProgress.add(GuidelineProgressResponse.WeekProgress.builder()
                    .weekNumber(guideline.getWeekNumber())
                    .title(guideline.getTitle())
                    .completed(isCompleted)
                    .completedChecklistCount(completedChecklist)
                    .totalChecklistCount(totalChecklist)
                    .build());
        }

        // 현재 주차 계산 (시즌 시작일 기준)
        Season season = template.getSeason();
        int currentWeek = calculateCurrentWeek(season.getProjectStartAt());

        double progressPercentage = template.getTotalWeeks() > 0 ?
                (double) completedWeeks / template.getTotalWeeks() * 100 : 0;

        return GuidelineProgressResponse.builder()
                .userId(userId)
                .seasonId(seasonId)
                .templateId(template.getId())
                .templateTitle(template.getTitle())
                .totalWeeks(template.getTotalWeeks())
                .completedWeeks(completedWeeks)
                .progressPercentage(Math.round(progressPercentage * 10) / 10.0)
                .currentWeek(Math.min(currentWeek, template.getTotalWeeks()))
                .weeklyProgress(weeklyProgress)
                .build();
    }

    // ========== Helper Methods ==========

    private int calculateWeeks(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(start, end);
        return (int) Math.ceil(days / 7.0);
    }

    private int calculateCurrentWeek(LocalDateTime projectStart) {
        if (projectStart == null) {
            return 1;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(projectStart)) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(projectStart, now);
        return (int) Math.ceil((days + 1) / 7.0);
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private int countChecklistItems(String checklistJson) {
        if (checklistJson == null || checklistJson.isBlank()) {
            return 0;
        }
        try {
            List<String> items = objectMapper.readValue(checklistJson, new TypeReference<List<String>>() {});
            return items.size();
        } catch (JsonProcessingException e) {
            return 0;
        }
    }

    private int countCompletedChecklist(String completedJson) {
        if (completedJson == null || completedJson.isBlank()) {
            return 0;
        }
        try {
            List<Integer> indexes = objectMapper.readValue(completedJson, new TypeReference<List<Integer>>() {});
            return indexes.size();
        } catch (JsonProcessingException e) {
            return 0;
        }
    }
}
