package com.mechuragi.mechuragi_server.domain.diary.validator;

import java.math.BigDecimal;

public class RatingValidator {

    private RatingValidator() {
        // 인스턴스화 방지
    }

    /**
     * 별점이 유효한지 검증
     * - 0.0 ~ 5.0 범위
     * - 0.5 단위 (0.0, 0.5, 1.0, 1.5, ..., 5.0)
     */
    public static boolean isValid(BigDecimal rating) {
        if (rating == null) {
            return false;
        }
        if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(new BigDecimal("5.0")) > 0) {
            return false;
        }
        // 0.5 단위 검증 (0.0, 0.5, 1.0, 1.5, ..., 5.0)
        BigDecimal doubled = rating.multiply(new BigDecimal("2"));
        return doubled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0;
    }
}
