package com.mechuragi.mechuragi_server.domain.member.entity;

import com.mechuragi.mechuragi_server.domain.member.entity.type.AuthProvider;
import com.mechuragi.mechuragi_server.domain.member.entity.type.MemberStatus;
import com.mechuragi.mechuragi_server.domain.member.entity.type.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(length = 500)
    private String password;

    @Column(length = 500)
    private String profileImageUrl;

    @Column
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(nullable = false)
    private Boolean voteNotificationEnabled = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Member(String email, String nickname, String password, String profileImageUrl,
                  Boolean emailVerified, AuthProvider provider, Role role, MemberStatus status,
                  Boolean voteNotificationEnabled) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.emailVerified = emailVerified != null ? emailVerified : false;
        this.provider = provider != null ? provider : AuthProvider.NORMAL;
        this.role = role != null ? role : Role.USER;
        this.status = status != null ? status : MemberStatus.ACTIVE;
        this.voteNotificationEnabled = voteNotificationEnabled != null ? voteNotificationEnabled : true;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void changeStatus(MemberStatus status) {
        this.status = status;
    }

    public void updateVoteNotificationSetting(Boolean enabled) {
        if (enabled != null) {
            this.voteNotificationEnabled = enabled;
        }
    }

    /**
     * 닉네임에 멤버 ID를 추가하여 업데이트
     * OAuth2 회원가입 시 랜덤 닉네임 + ID 조합을 위해 사용
     */
    public void appendIdToNickname(String baseNickname) {
        if (this.id != null) {
            this.nickname = baseNickname + this.id.intValue();
        }
    }
}