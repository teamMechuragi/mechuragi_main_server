package com.mechuragi.mechuragi_server.global.config;

import com.mechuragi.mechuragi_server.domain.user.entity.User;
import com.mechuragi.mechuragi_server.domain.user.repository.UserRepository;
import com.mechuragi.mechuragi_server.global.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;

    @Override
    public void run(String... args) throws Exception {
        // 테스트용 더미 사용자 생성
        createTestUsers();
    }

    private void createTestUsers() {
        // 이미 사용자가 있으면 생성하지 않음
        if (userRepository.count() > 0) {
            log.info("테스트 사용자가 이미 존재합니다.");
            return;
        }

        // 테스트용 사용자 1
        User testUser1 = User.builder()
                .email("test1@mechuragi.com")
                .nickname(nicknameGenerator.generateRandomNickname())
                .profileImageUrl("https://example.com/profile1.jpg")
                .build();

        // 테스트용 사용자 2
        User testUser2 = User.builder()
                .email("test2@mechuragi.com")
                .nickname(nicknameGenerator.generateRandomNickname())
                .profileImageUrl(null) // 프로필 이미지 없는 케이스
                .build();

        userRepository.save(testUser1);
        userRepository.save(testUser2);

        log.info("테스트 사용자 2명이 생성되었습니다.");
        log.info("사용자 1 - ID: {}, 닉네임: {}", testUser1.getId(), testUser1.getNickname());
        log.info("사용자 2 - ID: {}, 닉네임: {}", testUser2.getId(), testUser2.getNickname());
    }
}