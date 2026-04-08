package com.restartpoint.domain.matching.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.matching.dto.MemberRecommendationResponse;
import com.restartpoint.domain.matching.dto.TeamRecommendationResponse;
import com.restartpoint.domain.profile.dto.ProfileResponse;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.profile.entity.Profile;
import com.restartpoint.domain.profile.repository.ProfileRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.team.dto.TeamResponse;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.team.repository.TeamRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.infra.ai.ClaudeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TeamMatchingService {

    private final ClaudeService claudeService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SeasonRepository seasonRepository;
    private final ObjectMapper objectMapper;

    private static final String TEAM_RECOMMENDATION_SYSTEM_PROMPT = """
            당신은 프로젝트 팀 매칭 전문가입니다. 사용자의 프로필과 팀 정보를 분석하여 최적의 팀을 추천해야 합니다.

            추천 기준:
            1. 역할 매칭: 사용자의 역할이 팀에서 모집 중인 역할과 일치하는지
            2. 기술 스택 호환성: 팀 프로젝트에 사용자의 기술이 도움이 될지
            3. 관심 도메인 일치: 팀의 프로젝트 분야와 사용자의 관심 분야 일치도
            4. 시간 투자 가능성: 사용자의 가용 시간과 프로젝트 난이도 고려
            5. 협업 스타일 호환성: 팀 구성원들과의 협업 스타일 매칭

            응답은 반드시 JSON 배열 형식으로 해주세요. 각 팀에 대해 다음 정보를 포함하세요:
            {
                "teamId": 팀ID(숫자),
                "matchScore": 추천점수(0-100 정수),
                "reasons": ["추천 이유1", "추천 이유2", "추천 이유3"],
                "balanceAnalysis": "팀 밸런스 분석 내용",
                "scheduleRisk": "LOW" 또는 "MEDIUM" 또는 "HIGH",
                "missingRoles": ["부족한 역할1", "부족한 역할2"]
            }

            점수가 높은 순서로 정렬해서 응답해주세요.
            """;

    private static final String MEMBER_RECOMMENDATION_SYSTEM_PROMPT = """
            당신은 프로젝트 팀 매칭 전문가입니다. 팀 정보와 지원자 프로필을 분석하여 최적의 팀원을 추천해야 합니다.

            추천 기준:
            1. 역할 매칭: 지원자의 역할이 팀에서 모집 중인 역할과 일치하는지
            2. 기술 스택 보완성: 팀의 기존 기술 스택을 보완할 수 있는지
            3. 관심 도메인 일치: 팀의 프로젝트 분야와 지원자의 관심 분야 일치도
            4. 시간 투자 가능성: 지원자의 가용 시간과 프로젝트 요구사항 매칭
            5. 협업 스타일 호환성: 기존 팀원들과의 협업 스타일 매칭

            응답은 반드시 JSON 배열 형식으로 해주세요. 각 지원자에 대해 다음 정보를 포함하세요:
            {
                "profileId": 프로필ID(숫자),
                "matchScore": 추천점수(0-100 정수),
                "reasons": ["추천 이유1", "추천 이유2", "추천 이유3"],
                "balanceAnalysis": "팀 밸런스에 미치는 영향 분석",
                "scheduleRisk": "LOW" 또는 "MEDIUM" 또는 "HIGH",
                "complementarySkills": ["보완 가능한 스킬1", "보완 가능한 스킬2"]
            }

            점수가 높은 순서로 정렬해서 응답해주세요.
            """;

    /**
     * 사용자에게 맞는 팀 추천 (팀에 참가하려는 사용자용)
     */
    public List<TeamRecommendationResponse> recommendTeamsForUser(Long userId, Long seasonId, int limit) {
        User user = findUserById(userId);
        Profile userProfile = findProfileByUser(user);
        Season season = findSeasonById(seasonId);

        // 사용자가 이미 팀에 소속되어 있는지 확인
        if (isUserInTeam(user, season)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }

        // 모집 중인 팀 조회 (사용자 역할을 모집하는 팀)
        List<Team> recruitingTeams = teamRepository.findRecruitingTeamsBySeason(season).stream()
                .filter(team -> isTeamRecruitingRole(team, userProfile.getJobRole()))
                .filter(team -> !team.isFull())
                .toList();

        if (recruitingTeams.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MATCHING_CANDIDATES);
        }

        // AI 분석을 위한 데이터 준비
        String userPrompt = buildTeamRecommendationPrompt(userProfile, recruitingTeams);

        // Claude API 호출
        String aiResponse = claudeService.chat(TEAM_RECOMMENDATION_SYSTEM_PROMPT, userPrompt);

        if (aiResponse == null || aiResponse.isBlank()) {
            // AI 응답 실패 시 기본 추천 로직
            return buildDefaultTeamRecommendations(recruitingTeams, userProfile, limit);
        }

        // AI 응답 파싱 및 결과 매핑
        return parseTeamRecommendations(aiResponse, recruitingTeams, limit);
    }

    /**
     * 팀에 맞는 멤버 추천 (팀 리더용)
     */
    public List<MemberRecommendationResponse> recommendMembersForTeam(Long userId, Long teamId, int limit) {
        User leader = findUserById(userId);
        Team team = findTeamById(teamId);

        // 팀 리더인지 확인
        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new BusinessException(ErrorCode.NOT_TEAM_LEADER);
        }

        // 팀이 모집 중인지 확인
        if (team.isFull()) {
            throw new BusinessException(ErrorCode.TEAM_FULL);
        }

        // 팀에서 모집 중인 역할 확인
        List<JobRole> recruitingRoles = getRecruitingRoles(team);
        if (recruitingRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MATCHING_CANDIDATES);
        }

        // 해당 역할의 수료 인증된 프로필 조회 (이미 팀에 소속된 사용자 제외)
        List<Profile> candidateProfiles = recruitingRoles.stream()
                .flatMap(role -> profileRepository.findCertifiedProfilesByRole(role).stream())
                .filter(profile -> !isUserInTeam(profile.getUser(), team.getSeason()))
                .distinct()
                .toList();

        if (candidateProfiles.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MATCHING_CANDIDATES);
        }

        // AI 분석을 위한 데이터 준비
        String userPrompt = buildMemberRecommendationPrompt(team, candidateProfiles);

        // Claude API 호출
        String aiResponse = claudeService.chat(MEMBER_RECOMMENDATION_SYSTEM_PROMPT, userPrompt);

        if (aiResponse == null || aiResponse.isBlank()) {
            // AI 응답 실패 시 기본 추천 로직
            return buildDefaultMemberRecommendations(candidateProfiles, team, limit);
        }

        // AI 응답 파싱 및 결과 매핑
        return parseMemberRecommendations(aiResponse, candidateProfiles, limit);
    }

    private String buildTeamRecommendationPrompt(Profile userProfile, List<Team> teams) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 사용자 프로필\n");
        sb.append(String.format("- 이름: %s\n", userProfile.getUser().getName()));
        sb.append(String.format("- 역할: %s\n", userProfile.getJobRole()));
        sb.append(String.format("- 기술 스택: %s\n", String.join(", ", userProfile.getTechStacks())));
        sb.append(String.format("- 관심 도메인: %s\n", String.join(", ", userProfile.getInterestedDomains())));
        sb.append(String.format("- 주당 투자 가능 시간: %d시간\n",
                userProfile.getAvailableHoursPerWeek() != null ? userProfile.getAvailableHoursPerWeek() : 0));
        sb.append(String.format("- 협업 스타일: %s\n", userProfile.getCollaborationStyle()));
        sb.append(String.format("- 선호 난이도: %s\n", userProfile.getPreferredDifficulty()));
        sb.append(String.format("- 성장 목표: %s\n", userProfile.getImprovementGoal()));
        sb.append("\n## 모집 중인 팀 목록\n");

        for (Team team : teams) {
            sb.append(String.format("\n### 팀 ID: %d, 이름: %s\n", team.getId(), team.getName()));
            sb.append(String.format("- 설명: %s\n", team.getDescription() != null ? team.getDescription() : "없음"));
            sb.append(String.format("- 현재 멤버 수: %d/4\n", team.getMemberCount()));
            sb.append("- 모집 역할: ");
            List<String> recruiting = new ArrayList<>();
            if (Boolean.TRUE.equals(team.getRecruitingPlanner())) recruiting.add("기획자");
            if (Boolean.TRUE.equals(team.getRecruitingUxui())) recruiting.add("UX/UI 디자이너");
            if (Boolean.TRUE.equals(team.getRecruitingFrontend())) recruiting.add("프론트엔드");
            if (Boolean.TRUE.equals(team.getRecruitingBackend())) recruiting.add("백엔드");
            sb.append(String.join(", ", recruiting)).append("\n");

            // 팀 멤버들의 간략한 정보
            sb.append("- 현재 팀원: ");
            List<String> memberInfo = team.getMembers().stream()
                    .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                    .map(m -> String.format("%s(%s)", m.getUser().getName(), m.getRole()))
                    .toList();
            sb.append(String.join(", ", memberInfo)).append("\n");
        }

        return sb.toString();
    }

    private String buildMemberRecommendationPrompt(Team team, List<Profile> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 팀 정보\n");
        sb.append(String.format("- 팀명: %s\n", team.getName()));
        sb.append(String.format("- 설명: %s\n", team.getDescription() != null ? team.getDescription() : "없음"));
        sb.append(String.format("- 현재 멤버 수: %d/4\n", team.getMemberCount()));

        sb.append("- 모집 역할: ");
        List<String> recruiting = new ArrayList<>();
        if (Boolean.TRUE.equals(team.getRecruitingPlanner())) recruiting.add("기획자");
        if (Boolean.TRUE.equals(team.getRecruitingUxui())) recruiting.add("UX/UI 디자이너");
        if (Boolean.TRUE.equals(team.getRecruitingFrontend())) recruiting.add("프론트엔드");
        if (Boolean.TRUE.equals(team.getRecruitingBackend())) recruiting.add("백엔드");
        sb.append(String.join(", ", recruiting)).append("\n");

        sb.append("- 현재 팀원:\n");
        team.getMembers().stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                .forEach(m -> {
                    Profile memberProfile = profileRepository.findByUser(m.getUser()).orElse(null);
                    if (memberProfile != null) {
                        sb.append(String.format("  - %s (%s): %s\n",
                                m.getUser().getName(),
                                m.getRole(),
                                String.join(", ", memberProfile.getTechStacks())));
                    }
                });

        sb.append("\n## 지원 후보 프로필\n");
        for (Profile profile : candidates) {
            sb.append(String.format("\n### 프로필 ID: %d\n", profile.getId()));
            sb.append(String.format("- 이름: %s\n", profile.getUser().getName()));
            sb.append(String.format("- 역할: %s\n", profile.getJobRole()));
            sb.append(String.format("- 기술 스택: %s\n", String.join(", ", profile.getTechStacks())));
            sb.append(String.format("- 관심 도메인: %s\n", String.join(", ", profile.getInterestedDomains())));
            sb.append(String.format("- 주당 투자 가능 시간: %d시간\n",
                    profile.getAvailableHoursPerWeek() != null ? profile.getAvailableHoursPerWeek() : 0));
            sb.append(String.format("- 협업 스타일: %s\n", profile.getCollaborationStyle()));
            sb.append(String.format("- 선호 난이도: %s\n", profile.getPreferredDifficulty()));
            sb.append(String.format("- 성장 목표: %s\n", profile.getImprovementGoal()));
            sb.append(String.format("- 자기소개: %s\n", profile.getIntroduction()));
        }

        return sb.toString();
    }

    private List<TeamRecommendationResponse> parseTeamRecommendations(
            String aiResponse, List<Team> teams, int limit) {
        try {
            // JSON 배열 추출
            String jsonArray = extractJsonArray(aiResponse);
            List<Map<String, Object>> recommendations = objectMapper.readValue(
                    jsonArray, new TypeReference<List<Map<String, Object>>>() {});

            Map<Long, Team> teamMap = teams.stream()
                    .collect(Collectors.toMap(Team::getId, t -> t));

            return recommendations.stream()
                    .filter(rec -> teamMap.containsKey(((Number) rec.get("teamId")).longValue()))
                    .limit(limit)
                    .map(rec -> {
                        Team team = teamMap.get(((Number) rec.get("teamId")).longValue());
                        return TeamRecommendationResponse.builder()
                                .team(TeamResponse.simpleFrom(team))
                                .matchScore(((Number) rec.get("matchScore")).intValue())
                                .reasons((List<String>) rec.get("reasons"))
                                .balanceAnalysis((String) rec.get("balanceAnalysis"))
                                .scheduleRisk((String) rec.get("scheduleRisk"))
                                .missingRoles((List<String>) rec.get("missingRoles"))
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", e.getMessage());
            return buildDefaultTeamRecommendations(teams, null, limit);
        }
    }

    private List<MemberRecommendationResponse> parseMemberRecommendations(
            String aiResponse, List<Profile> profiles, int limit) {
        try {
            // JSON 배열 추출
            String jsonArray = extractJsonArray(aiResponse);
            List<Map<String, Object>> recommendations = objectMapper.readValue(
                    jsonArray, new TypeReference<List<Map<String, Object>>>() {});

            Map<Long, Profile> profileMap = profiles.stream()
                    .collect(Collectors.toMap(Profile::getId, p -> p));

            return recommendations.stream()
                    .filter(rec -> profileMap.containsKey(((Number) rec.get("profileId")).longValue()))
                    .limit(limit)
                    .map(rec -> {
                        Profile profile = profileMap.get(((Number) rec.get("profileId")).longValue());
                        return MemberRecommendationResponse.builder()
                                .profile(ProfileResponse.from(profile))
                                .matchScore(((Number) rec.get("matchScore")).intValue())
                                .reasons((List<String>) rec.get("reasons"))
                                .balanceAnalysis((String) rec.get("balanceAnalysis"))
                                .scheduleRisk((String) rec.get("scheduleRisk"))
                                .complementarySkills((List<String>) rec.get("complementarySkills"))
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", e.getMessage());
            return buildDefaultMemberRecommendations(profiles, null, limit);
        }
    }

    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        return "[]";
    }

    private List<TeamRecommendationResponse> buildDefaultTeamRecommendations(
            List<Team> teams, Profile userProfile, int limit) {
        return teams.stream()
                .limit(limit)
                .map(team -> TeamRecommendationResponse.builder()
                        .team(TeamResponse.simpleFrom(team))
                        .matchScore(70) // 기본 점수
                        .reasons(List.of(
                                "팀에서 해당 역할을 모집 중입니다.",
                                "팀에 합류 가능한 자리가 있습니다.",
                                "같은 시즌의 프로젝트입니다."
                        ))
                        .balanceAnalysis("AI 분석을 사용할 수 없어 기본 추천을 제공합니다.")
                        .scheduleRisk("MEDIUM")
                        .missingRoles(List.of())
                        .build())
                .toList();
    }

    private List<MemberRecommendationResponse> buildDefaultMemberRecommendations(
            List<Profile> profiles, Team team, int limit) {
        return profiles.stream()
                .limit(limit)
                .map(profile -> MemberRecommendationResponse.builder()
                        .profile(ProfileResponse.from(profile))
                        .matchScore(70) // 기본 점수
                        .reasons(List.of(
                                "팀에서 모집 중인 역할과 일치합니다.",
                                "수료 인증이 완료된 사용자입니다.",
                                "프로필 정보가 등록되어 있습니다."
                        ))
                        .balanceAnalysis("AI 분석을 사용할 수 없어 기본 추천을 제공합니다.")
                        .scheduleRisk("MEDIUM")
                        .complementarySkills(new ArrayList<>(profile.getTechStacks()))
                        .build())
                .toList();
    }

    private boolean isTeamRecruitingRole(Team team, JobRole role) {
        return switch (role) {
            case PLANNER -> Boolean.TRUE.equals(team.getRecruitingPlanner());
            case UXUI -> Boolean.TRUE.equals(team.getRecruitingUxui());
            case FRONTEND -> Boolean.TRUE.equals(team.getRecruitingFrontend());
            case BACKEND -> Boolean.TRUE.equals(team.getRecruitingBackend());
        };
    }

    private List<JobRole> getRecruitingRoles(Team team) {
        List<JobRole> roles = new ArrayList<>();
        if (Boolean.TRUE.equals(team.getRecruitingPlanner())) roles.add(JobRole.PLANNER);
        if (Boolean.TRUE.equals(team.getRecruitingUxui())) roles.add(JobRole.UXUI);
        if (Boolean.TRUE.equals(team.getRecruitingFrontend())) roles.add(JobRole.FRONTEND);
        if (Boolean.TRUE.equals(team.getRecruitingBackend())) roles.add(JobRole.BACKEND);
        return roles;
    }

    private boolean isUserInTeam(User user, Season season) {
        boolean isLeader = teamRepository.findByLeader(user).stream()
                .anyMatch(t -> t.getSeason().getId().equals(season.getId()));
        if (isLeader) return true;
        return teamMemberRepository.existsAcceptedMemberInSeason(user, season);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Profile findProfileByUser(User user) {
        return profileRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
    }

    private Season findSeasonById(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));
    }

    private Team findTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }
}
