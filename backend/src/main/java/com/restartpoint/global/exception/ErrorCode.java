package com.restartpoint.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 입력값입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_003", "요청한 리소스를 찾을 수 없습니다."),

    // 인증 에러
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_004", "접근 권한이 없습니다."),

    // 사용자 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_003", "비밀번호가 일치하지 않습니다."),
    CERTIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "USER_004", "수료 인증이 필요합니다."),
    CERTIFICATION_PENDING(HttpStatus.FORBIDDEN, "USER_005", "수료 인증 승인 대기 중입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "USER_006", "이메일 인증이 필요합니다."),
    EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "USER_007", "이미 인증된 이메일입니다."),

    // 이메일 인증 에러
    VERIFICATION_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFY_001", "인증 코드를 찾을 수 없습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "VERIFY_002", "인증 코드가 일치하지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "VERIFY_003", "인증 코드가 만료되었습니다."),
    SIGNUP_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFY_004", "회원가입 세션을 찾을 수 없습니다."),
    SIGNUP_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "VERIFY_005", "회원가입 세션이 만료되었습니다."),

    // 프로필 에러
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE_001", "프로필을 찾을 수 없습니다."),
    PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROFILE_002", "이미 프로필이 존재합니다."),

    // 시즌 에러
    SEASON_NOT_FOUND(HttpStatus.NOT_FOUND, "SEASON_001", "시즌을 찾을 수 없습니다."),
    SEASON_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "SEASON_002", "현재 모집 중인 시즌이 아닙니다."),
    SEASON_NOT_TEAM_BUILDING(HttpStatus.BAD_REQUEST, "SEASON_003", "현재 팀빌딩 기간이 아닙니다."),
    INVALID_SEASON_STATUS(HttpStatus.BAD_REQUEST, "SEASON_004", "해당 상태에서는 이 작업을 수행할 수 없습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "SEASON_005", "날짜 범위가 올바르지 않습니다."),
    INVALID_REVIEW_WEIGHT(HttpStatus.BAD_REQUEST, "SEASON_006", "심사 비중의 합이 100%여야 합니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "SEASON_007", "유효하지 않은 상태 전환입니다."),

    // 팀 에러
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM_001", "팀을 찾을 수 없습니다."),
    TEAM_FULL(HttpStatus.BAD_REQUEST, "TEAM_002", "팀 정원이 가득 찼습니다."),
    ALREADY_IN_TEAM(HttpStatus.CONFLICT, "TEAM_003", "이미 팀에 소속되어 있습니다."),
    NOT_TEAM_LEADER(HttpStatus.FORBIDDEN, "TEAM_004", "팀 리더만 수행할 수 있는 작업입니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "TEAM_005", "이미 지원한 팀입니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM_006", "지원 내역을 찾을 수 없습니다."),
    TEAM_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "TEAM_007", "모집 중인 팀이 아닙니다."),

    // 프로젝트 에러
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_001", "프로젝트를 찾을 수 없습니다."),
    PROJECT_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROJECT_002", "이미 프로젝트가 존재합니다."),
    INVALID_PROJECT_STATUS(HttpStatus.BAD_REQUEST, "PROJECT_003", "해당 상태에서는 이 작업을 수행할 수 없습니다."),
    PROJECT_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "PROJECT_004", "이미 제출된 프로젝트입니다."),
    SUBMISSION_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "PROJECT_005", "제출 마감이 지났습니다."),
    SEASON_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "PROJECT_006", "현재 프로젝트 진행 기간이 아닙니다."),
    NOT_TEAM_MEMBER(HttpStatus.FORBIDDEN, "PROJECT_007", "팀원만 수행할 수 있는 작업입니다."),

    // 체크포인트 에러
    CHECKPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHECKPOINT_001", "체크포인트를 찾을 수 없습니다."),
    CHECKPOINT_ALREADY_EXISTS(HttpStatus.CONFLICT, "CHECKPOINT_002", "해당 주차에 이미 체크포인트가 존재합니다."),

    // AI 에러
    AI_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI_001", "AI 서비스에 일시적인 문제가 발생했습니다."),
    NO_MATCHING_CANDIDATES(HttpStatus.NOT_FOUND, "AI_002", "추천 가능한 후보가 없습니다."),
    AI_FEEDBACK_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI_003", "AI 피드백 생성에 실패했습니다."),

    // 심사 에러
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_001", "심사를 찾을 수 없습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_002", "이미 해당 프로젝트에 심사를 제출했습니다."),
    SEASON_NOT_REVIEWING(HttpStatus.BAD_REQUEST, "REVIEW_003", "현재 심사 기간이 아닙니다."),
    PROJECT_NOT_SUBMITTED(HttpStatus.BAD_REQUEST, "REVIEW_004", "제출되지 않은 프로젝트는 심사할 수 없습니다."),
    CANNOT_REVIEW_OWN_PROJECT(HttpStatus.FORBIDDEN, "REVIEW_005", "자신의 프로젝트는 심사할 수 없습니다."),
    INVALID_RUBRIC_SCORES(HttpStatus.BAD_REQUEST, "REVIEW_006", "모든 루브릭 항목에 점수를 입력해야 합니다."),
    NOT_CERTIFIED_REVIEWER(HttpStatus.FORBIDDEN, "REVIEW_007", "수료 인증이 완료된 사용자만 심사할 수 있습니다."),
    GUIDE_NOT_COMPLETED(HttpStatus.FORBIDDEN, "REVIEW_008", "심사 가이드 학습을 먼저 완료해야 합니다."),
    PREVIOUS_STEP_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "REVIEW_009", "이전 단계를 먼저 완료해야 합니다."),

    // 성장 리포트 에러
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_001", "성장 리포트를 찾을 수 없습니다."),
    REPORT_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "REPORT_002", "리포트 생성에 실패했습니다."),

    // 커뮤니티 에러
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_001", "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_002", "댓글을 찾을 수 없습니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "COMMUNITY_003", "게시글 작성자만 수행할 수 있는 작업입니다."),
    NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "COMMUNITY_004", "댓글 작성자만 수행할 수 있는 작업입니다."),
    ANNOUNCEMENT_ADMIN_ONLY(HttpStatus.FORBIDDEN, "COMMUNITY_005", "공지는 관리자만 작성할 수 있습니다."),
    SHOWCASE_REQUIRES_PROJECT(HttpStatus.BAD_REQUEST, "COMMUNITY_006", "쇼케이스는 프로젝트 연결이 필요합니다."),
    COMMENT_NOT_BELONG_TO_POST(HttpStatus.BAD_REQUEST, "COMMUNITY_007", "해당 댓글은 이 게시글에 속하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
