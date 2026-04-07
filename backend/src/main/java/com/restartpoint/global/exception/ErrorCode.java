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
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM_006", "지원 내역을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
