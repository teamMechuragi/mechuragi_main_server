package com.mechuragi.mechuragi_server.auth.service;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.auth.dto.OAuth2Attributes;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * OAuth2 로그인 시 사용자 정보를 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;
    private final NicknameGenerator nicknameGenerator;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. OAuth2 사용자 정보 로드
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 어떤 OAuth2 제공자인지 확인 (kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. OAuth2 로그인 시 키가 되는 필드 값 (PK와 같은 의미)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        // 4. OAuth2User의 attribute를 담을 클래스
        OAuth2Attributes attributes = OAuth2Attributes.of(
                registrationId,
                userNameAttributeName,
                oAuth2User.getAttributes()
        );

        // 5. 회원 정보 조회 또는 생성
        Member member;
        try {
            member = saveOrUpdate(attributes);
        } catch (BusinessException ex) {
            // BusinessException은 AuthenticationException이 아니므로 Spring Security 필터가 잡지 못함.
            // OAuth2AuthenticationException으로 감싸야 OAuth2FailureHandler까지 전달된다.
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ex.getErrorCode().getCode()),
                    ex.getMessage(),
                    ex
            );
        }

        log.info("OAuth2 로그인 성공: email={}, provider={}", member.getEmail(), member.getProvider());

        // 6. CustomUserDetails 반환 (Spring Security가 인식할 수 있는 형태)
        return new CustomUserDetails(member, oAuth2User.getAttributes());
    }

    /**
     * 회원 정보 저장 또는 업데이트
     */
    private Member saveOrUpdate(OAuth2Attributes attributes) {
        Optional<Member> existingMember = memberRepository.findByEmail(attributes.getEmail());

        if (existingMember.isPresent()) {
            // 기존 회원이면 정보 조회만 수행 (OAuth2 로그인으로는 프로필 업데이트 안함)
            Member member = existingMember.get();

            // 소셜 로그인 제공자 확인
            if (member.getProvider() != attributes.getProvider()) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS); // 가입된 일반 계정 존재
            }

            log.info("기존 회원 로그인: email={}", member.getEmail());
            return member;
        } else {
            // 신규 회원이면 저장
            // 1. 랜덤 닉네임 생성 (예: "행복한곰")
            String randomNickname = nicknameGenerator.generateRandomNickname();

            // 2. 임시 닉네임으로 Member 엔티티 생성 및 저장 (ID 발급을 위해)
            Member newMember = attributes.toEntity(randomNickname);
            Member savedMember = memberRepository.save(newMember);

            // 3. ID를 포함한 최종 닉네임으로 업데이트 (예: "행복한곰1")
            savedMember.appendIdToNickname(randomNickname);

            log.info("신규 소셜 회원 가입: email={}, nickname={}, provider={}",
                    savedMember.getEmail(), savedMember.getNickname(), savedMember.getProvider());
            return savedMember;
        }
    }
}
