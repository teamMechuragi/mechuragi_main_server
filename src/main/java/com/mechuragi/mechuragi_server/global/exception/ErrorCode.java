package com.mechuragi.mechuragi_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "사용자를 찾을 수 없습니다."),

    // Auth
    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효한 JWT 토큰이 없습니다"),

    // Preference
    PREFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "음식 취향을 찾을 수 없습니다."),
    PREFERENCE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P002", "해당 음식 취향에 접근할 권한이 없습니다."),

    // AI
    AI_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI001", "AI 서비스 통신 중 오류가 발생했습니다"),

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}