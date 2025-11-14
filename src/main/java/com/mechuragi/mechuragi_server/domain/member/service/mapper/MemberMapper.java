package com.mechuragi.mechuragi_server.domain.member.service.mapper;

import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import com.mechuragi.mechuragi_server.domain.member.dto.SignupRequest;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.entity.type.AuthProvider;
import com.mechuragi.mechuragi_server.domain.member.entity.type.MemberStatus;
import com.mechuragi.mechuragi_server.domain.member.entity.type.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberMapper {

    private final PasswordEncoder passwordEncoder;

    /**
     * SignupRequest를 Member 엔티티로 변환
     */
    public Member toEntity(SignupRequest request) {
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        return Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname()) // 임시 닉네임
                .emailVerified(true) // 이메일 인증 완료 - 추후 제대로 구현
                .provider(AuthProvider.NORMAL)
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    /**
     * Member 엔티티를 MemberResponse DTO로 변환
     */
    public MemberResponse toDto(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .emailVerified(member.getEmailVerified())
                .provider(member.getProvider().name())
                .role(member.getRole().name())
                .status(member.getStatus().name())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}
