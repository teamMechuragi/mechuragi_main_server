package com.mechuragi.mechuragi_server.auth.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

// 닉네임 null 방지용 - 소셜 로그인에서만 사용
@Component
public class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "배고픈", "신나는", "귀여운", "맛있는", "달콤한",
            "매콤한", "상큼한", "포근한", "활발한", "즐거운",
            "행복한", "멋진", "따뜻한", "시원한", "깔끔한",
            "든든한", "푸짐한", "고소한", "새콤한", "바삭한"
    );

    private static final List<String> NOUNS = List.of(
            "곰", "치킨", "피자", "라면", "김치",
            "햄버거", "도넛", "케이크", "아이스크림", "과자",
            "떡볶이", "순대", "호떡", "붕어빵", "타코야키",
            "만두", "국수", "비빔밥", "초밥", "파스타"
    );

    private final Random random = new Random();

    public String generateRandomNickname() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        return adjective + noun;
    }
}