package com.mechuragi.mechuragi_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "M002", "가입된 이메일이 존재합니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "M003", "중복된 닉네임입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST,"M004","비밀번호가 일치하지 않습니다."),
    SOCIAL_ACCOUNT_EXISTS(HttpStatus.CONFLICT,"M005","가입된 소셜로그인 계정이 존재합니다."),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN,"M006","활성화된 계정이 아닙니다."),

    // Email
    EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "E001", "인증 코드가 일치하지 않습니다."),
    EMAIL_VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "E002", "인증 코드가 만료되었습니다."),
    EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "E003", "이미 인증 완료된 이메일입니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E004", "이메일 발송에 실패했습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "E005", "이메일 인증이 완료되지 않았습니다."),

    // Auth
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A002", "Refresh Token을 찾을 수 없습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "만료된 Refresh Token입니다."),
    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 JWT 토큰입니다."),
    PASSWORD_CHANGE_DENIED(HttpStatus.BAD_REQUEST, "A005", "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다."),

    // Preference
    PREFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "음식 취향을 찾을 수 없습니다."),
    PREFERENCE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P002", "해당 음식 취향에 접근할 권한이 없습니다."),

    // AI
    AI_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI001", "AI 서비스 통신 중 오류가 발생했습니다"),
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "AI002", "추천 결과를 찾을 수 없습니다."),
    SCRAP_FORBIDDEN(HttpStatus.FORBIDDEN, "AI003", "해당 스크랩에 접근할 권한이 없습니다."),

    // Vote
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "V001", "투표를 찾을 수 없습니다."),
    VOTE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "V002", "이미 완료된 투표입니다."),
    VOTE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "V003", "투표 옵션을 찾을 수 없습니다."),
    VOTE_ALREADY_PARTICIPATED(HttpStatus.BAD_REQUEST, "V004", "이미 투표에 참여했습니다."),
    VOTE_EXPIRED(HttpStatus.BAD_REQUEST, "V005", "투표 기간이 만료되었습니다."),
    MULTIPLE_CHOICE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "V006", "복수 선택이 허용되지 않는 투표입니다."),
    VOTE_PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "V007", "투표 참여 기록을 찾을 수 없습니다."),
    VOTE_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "V008", "댓글을 찾을 수 없습니다."),
    VOTE_COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "V009", "댓글에 대한 권한이 없습니다."),

    // File
    FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "F001", "파일을 찾을 수 없습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "F002", "파일 크기가 너무 큽니다. (최대 5MB)"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "F003", "지원하지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F004", "파일 업로드에 실패했습니다."),
    INVALID_FILE_URL(HttpStatus.BAD_REQUEST, "F005", "유효하지 않은 파일 URL입니다."),

    // Diary
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "일기를 찾을 수 없습니다."),
    DIARY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "D002", "해당 일기에 접근할 권한이 없습니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, "D003", "별점은 0.0에서 5.0 사이여야 하며, 0.5 단위로만 입력 가능합니다."),
    DUPLICATE_DIARY_DATE(HttpStatus.BAD_REQUEST, "D004", "해당 날짜에 이미 일기가 존재합니다."),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "D005", "이미지는 최대 4장까지 첨부 가능합니다."),
    FUTURE_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "D006", "미래 날짜에는 일기를 작성할 수 없습니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "N002", "해당 알림에 접근할 권한이 없습니다."),

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}