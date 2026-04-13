package com.restartpoint.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.project.repository.CheckpointRepository;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.team.repository.TeamRepository;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private CheckpointRepository checkpointRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TeamRepository teamRepository;

    @MockBean
    private S3Client s3Client;

    @MockBean
    private S3Presigner s3Presigner;

    @BeforeEach
    void setUp() {
        // 외래 키 순서에 따라 삭제
        checkpointRepository.deleteAll();
        projectRepository.deleteAll();
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        seasonRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("공개 인증 API는 토큰 없이도 접근할 수 있다")
    void authEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "invalid-email",
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_002"));
    }

    @Test
    @DisplayName("보호된 API는 토큰이 없으면 접근할 수 없다")
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("유효한 사용자 토큰이 있으면 내 정보 조회가 가능하다")
    void authenticatedUserCanAccessProtectedEndpoint() throws Exception {
        User savedUser = userRepository.save(User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .name("일반유저")
                .role(Role.USER)
                .build());
        String token = jwtTokenProvider.createToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    @DisplayName("관리자 전용 API는 일반 사용자 토큰으로 접근할 수 없다")
    void adminEndpointRejectsNormalUser() throws Exception {
        User savedUser = userRepository.save(User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .name("일반유저")
                .role(Role.USER)
                .build());
        String token = jwtTokenProvider.createToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        mockMvc.perform(get("/api/v1/admin/users/certifications/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("공개 시즌 상세 API는 DRAFT 시즌에 접근할 수 없다")
    void publicSeasonDetailRejectsDraftSeason() throws Exception {
        Season draftSeason = seasonRepository.save(Season.builder()
                .title("비공개 시즌")
                .description("초안")
                .recruitmentStartAt(java.time.LocalDateTime.of(2026, 4, 1, 0, 0))
                .recruitmentEndAt(java.time.LocalDateTime.of(2026, 4, 7, 0, 0))
                .teamBuildingStartAt(java.time.LocalDateTime.of(2026, 4, 8, 0, 0))
                .teamBuildingEndAt(java.time.LocalDateTime.of(2026, 4, 14, 0, 0))
                .projectStartAt(java.time.LocalDateTime.of(2026, 4, 15, 0, 0))
                .projectEndAt(java.time.LocalDateTime.of(2026, 4, 30, 0, 0))
                .reviewStartAt(java.time.LocalDateTime.of(2026, 5, 1, 0, 0))
                .reviewEndAt(java.time.LocalDateTime.of(2026, 5, 7, 0, 0))
                .expertReviewWeight(100)
                .candidateReviewWeight(0)
                .build());

        mockMvc.perform(get("/api/v1/seasons/" + draftSeason.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SEASON_001"));
    }

    @Test
    @DisplayName("관리자 토큰이 있으면 운영자 시즌 상세 API로 DRAFT 시즌을 조회할 수 있다")
    void adminCanAccessDraftSeasonDetail() throws Exception {
        User adminUser = userRepository.save(User.builder()
                .email("admin@example.com")
                .password("encoded-password")
                .name("관리자")
                .role(Role.ADMIN)
                .build());
        Season draftSeason = seasonRepository.save(Season.builder()
                .title("비공개 시즌")
                .description("초안")
                .recruitmentStartAt(java.time.LocalDateTime.of(2026, 4, 1, 0, 0))
                .recruitmentEndAt(java.time.LocalDateTime.of(2026, 4, 7, 0, 0))
                .teamBuildingStartAt(java.time.LocalDateTime.of(2026, 4, 8, 0, 0))
                .teamBuildingEndAt(java.time.LocalDateTime.of(2026, 4, 14, 0, 0))
                .projectStartAt(java.time.LocalDateTime.of(2026, 4, 15, 0, 0))
                .projectEndAt(java.time.LocalDateTime.of(2026, 4, 30, 0, 0))
                .reviewStartAt(java.time.LocalDateTime.of(2026, 5, 1, 0, 0))
                .reviewEndAt(java.time.LocalDateTime.of(2026, 5, 7, 0, 0))
                .expertReviewWeight(100)
                .candidateReviewWeight(0)
                .build());
        String token = jwtTokenProvider.createToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());

        mockMvc.perform(get("/api/v1/admin/seasons/" + draftSeason.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(draftSeason.getId()))
                .andExpect(jsonPath("$.data.status").value(SeasonStatus.DRAFT.name()));
    }
}
