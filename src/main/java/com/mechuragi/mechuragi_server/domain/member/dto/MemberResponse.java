package com.mechuragi.mechuragi_server.domain.member.dto;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MemberResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private Boolean emailVerified;
    private String provider;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getEmailVerified(),
                member.getProvider().name(),
                member.getRole().name(),
                member.getStatus().name(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
