package com.mechuragi.mechuragi_server.auth.dto;

import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private TokenResponse tokens;
    private MemberResponse member;

    public static LoginResponse of(TokenResponse tokens, MemberResponse member) {
        return LoginResponse.builder()
                .tokens(tokens)
                .member(member)
                .build();
    }
}
