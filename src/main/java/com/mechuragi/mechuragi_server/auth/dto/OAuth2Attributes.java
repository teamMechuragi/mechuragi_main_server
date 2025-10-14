package com.mechuragi.mechuragi_server.auth.dto;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.entity.type.AuthProvider;
import com.mechuragi.mechuragi_server.domain.member.entity.type.MemberStatus;
import com.mechuragi.mechuragi_server.domain.member.entity.type.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * OAuth2 로그인 시 받아오는 사용자 정보를 담는 DTO
 */
@Getter
@Builder
public class OAuth2Attributes {

    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private AuthProvider provider;

    /**
     * OAuth2User의 attributes를 Provider에 따라 변환
     */
    public static OAuth2Attributes of(String registrationId,
                                      String userNameAttributeName,
                                      Map<String, Object> attributes) {
        // 카카오
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }

        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
    }

    /**
     * 카카오 로그인 정보 변환
     */
    private static OAuth2Attributes ofKakao(String userNameAttributeName,
                                             Map<String, Object> attributes) {
        // 카카오 계정 정보
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuth2Attributes.builder()
                .email((String) kakaoAccount.get("email"))
                .nickname((String) profile.get("nickname"))
                .profileImageUrl((String) profile.get("profile_image_url"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .provider(AuthProvider.KAKAO)
                .build();
    }

    /**
     * OAuth2Attributes를 Member 엔티티로 변환 (첫 가입 시)
     */
    public Member toEntity() {
        return Member.builder()
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .password(null) // 소셜 로그인은 비밀번호 없음
                .emailVerified(true) // 소셜 로그인은 이메일 인증 완료로 간주
                .provider(provider)
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
