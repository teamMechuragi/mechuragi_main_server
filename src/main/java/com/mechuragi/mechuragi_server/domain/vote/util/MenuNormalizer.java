package com.mechuragi.mechuragi_server.domain.vote.util;

import java.util.Map;

public class MenuNormalizer {

    // 동의어 테이블 (초기: 하드코딩, 추후 외부화 가능)
    private static final Map<String, String> SYNONYM_MAP = Map.ofEntries(
            Map.entry("파스타", "스파게티"),
            Map.entry("짜장면", "자장면"),
            Map.entry("라면", "라멘"),
            Map.entry("돈까스", "돈카츠"),
            Map.entry("떡볶이", "떡복이"),
            Map.entry("김밥", "kimbap"),
            Map.entry("gimbap", "kimbap"),
            Map.entry("치킨", "chicken"),
            Map.entry("피자", "pizza"),
            Map.entry("햄버거", "burger"),
            Map.entry("버거", "burger")
    );

    /**
     * 메뉴명 정규화 및 동의어 처리
     *
     * @param menuText 원본 메뉴명
     * @return 정규화된 메뉴명
     */
    public static String normalize(String menuText) {
        if (menuText == null || menuText.isBlank()) {
            return "";
        }

        // Step 1: 기본 정규화
        String normalized = normalizeText(menuText);

        // Step 2: 동의어 처리
        String canonical = applySynonym(normalized);

        return canonical;
    }

    /**
     * 문자열 정규화
     * - 공백 제거
     * - 특수문자 제거
     * - 이모지 제거
     * - 대소문자 통일 (소문자)
     */
    private static String normalizeText(String text) {
        // 1. 앞뒤 공백 제거
        String result = text.trim();

        // 2. 이모지 제거 (Unicode ranges for emojis)
        result = result.replaceAll("[\\p{So}\\p{Sk}]", "");

        // 3. 특수문자 제거 (알파벳, 숫자, 한글만 남김)
        result = result.replaceAll("[^a-zA-Z0-9가-힣\\s]", "");

        // 4. 연속된 공백을 단일 공백으로
        result = result.replaceAll("\\s+", " ");

        // 5. 다시 앞뒤 공백 제거
        result = result.trim();

        // 6. 소문자로 통일
        result = result.toLowerCase();

        return result;
    }

    /**
     * 동의어 처리
     * 정규화된 메뉴명을 표준 형태로 변환
     */
    private static String applySynonym(String normalizedText) {
        return SYNONYM_MAP.getOrDefault(normalizedText, normalizedText);
    }

    /**
     * 동의어 맵에 새로운 동의어 추가 (추후 Admin API용)
     * 현재는 static final이므로 런타임 추가 불가
     * Phase 2/3에서 외부 설정 또는 DB 기반으로 변경 필요
     */
    // public static void addSynonym(String from, String to) {
    //     // 추후 구현 (외부 설정 또는 DB 연동)
    // }
}
