package com.mechuragi.mechuragi_server.global.config;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.auth.service.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final NicknameGenerator nicknameGenerator;

    @Override
    public void run(String... args) throws Exception {
        // 테스트용 더미 사용자 생성
        createTestMembers();
    }

    private void createTestMembers() {
        // 이미 사용자가 있으면 생성하지 않음
        if (memberRepository.count() > 0) {
            log.info("테스트 사용자가 이미 존재합니다.");
            return;
        }

        // 테스트용 사용자 1
        Member testMember1 = Member.builder()
                .email("mechuragi001@gmail.com")
                .nickname(nicknameGenerator.generateRandomNickname())
                .profileImageUrl("https://example.com/profile1.jpg")
                .build();

        // 테스트용 사용자 2
        Member testMember2 = Member.builder()
                .email("mechragi@mechuragi.com")
                .nickname(nicknameGenerator.generateRandomNickname())
                .profileImageUrl(null) // 프로필 이미지 없는 케이스
                .build();

        memberRepository.save(testMember1);
        memberRepository.save(testMember2);

        log.info("테스트 사용자 2명이 생성되었습니다.");
        log.info("사용자 1 - ID: {}, 닉네임: {}", testMember1.getId(), testMember1.getNickname());
        log.info("사용자 2 - ID: {}, 닉네임: {}", testMember2.getId(), testMember2.getNickname());
    }
}