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

    // Vote
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "V001", "투표를 찾을 수 없습니다."),
    VOTE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "V002", "이미 완료된 투표입니다."),
    VOTE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "V003", "투표 옵션을 찾을 수 없습니다."),
    VOTE_ALREADY_PARTICIPATED(HttpStatus.BAD_REQUEST, "V004", "이미 투표에 참여했습니다."),
    VOTE_EXPIRED(HttpStatus.BAD_REQUEST, "V005", "투표 기간이 만료되었습니다."),
    MULTIPLE_CHOICE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "V006", "복수 선택이 허용되지 않는 투표입니다."),
    VOTE_PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "V007", "투표 참여 기록을 찾을 수 없습니다."),

    // File
    FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "F001", "파일을 찾을 수 없습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "F002", "파일 크기가 너무 큽니다. (최대 5MB)"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "F003", "지원하지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F004", "파일 업로드에 실패했습니다."),
    INVALID_FILE_URL(HttpStatus.BAD_REQUEST, "F005", "유효하지 않은 파일 URL입니다."),

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}