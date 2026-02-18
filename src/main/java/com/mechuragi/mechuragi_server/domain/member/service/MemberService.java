package com.mechuragi.mechuragi_server.domain.member.service;

import com.mechuragi.mechuragi_server.global.email.service.EmailService;
import com.mechuragi.mechuragi_server.domain.member.dto.*;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.entity.type.MemberStatus;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.member.service.mapper.MemberMapper;
import com.mechuragi.mechuragi_server.domain.member.entity.type.AuthProvider;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import com.mechuragi.mechuragi_server.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;
    private final S3Service s3Service;
    private final EmailService emailService;

    // 일반 회원가입
    @Transactional
    public MemberResponse signup(SignupRequest request) {

        // 이메일 중복 확인 및 소셜 계정 여부 체크
        memberRepository.findByEmail(request.getEmail()).ifPresent(existingMember -> {
            if (existingMember.getProvider() != AuthProvider.NORMAL) {
                throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_EXISTS);
            }
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        });

        // 회원 엔티티 생성 (Mapper 사용)
        Member member = memberMapper.toEntity(request);

        // 회원 저장
        Member savedMember = memberRepository.save(member);

        log.info("회원가입 완료: email={}, nickname={}", request.getEmail(), savedMember.getNickname());

        // 회원가입 환영 메일 발송
        emailService.sendWelcomeEmail(savedMember.getEmail(), savedMember.getNickname());

        return memberMapper.toDto(savedMember);
    }

    // 회원 조회 (ID)
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return memberMapper.toDto(member);
    }

    // 회원 조회 (이메일)
    public MemberResponse getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return memberMapper.toDto(member);
    }

    // 내 알림 설정 상태 조회
    public NotificationSettingResponse getNotificationSetting(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        boolean enabled = member.getVoteNotificationEnabled();
        return new NotificationSettingResponse(enabled);
    }

    // 프로필 이미지 변경
    @Transactional
    public void updateProfileImage(Long memberId, String imageUrl) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getProfileImageUrl() != null) {
            s3Service.deleteImage(member.getProfileImageUrl());
        }

        member.updateProfileImage(imageUrl);
    }

    // 프로필 닉네임 변경
    @Transactional
    public MemberResponse updateNickname(Long memberId, UpdateNicknameRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!request.getNickname().equals(member.getNickname())
                && memberRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }

        member.updateNickname(request.getNickname());
        return memberMapper.toDto(member);
    }

    // 비밀번호 변경
    @Transactional
    public void updatePassword(Long memberId, UpdatePasswordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 소셜 로그인 회원은 비밀번호 변경 불가
        if (member.getPassword() == null) {
            throw new BusinessException(ErrorCode.PASSWORD_CHANGE_DENIED);
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 새 비밀번호 암호화 및 저장
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        member.updatePassword(encodedPassword);
    }

    // 비밀번호 재설정 (비로그인 상태)
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 이메일 인증 여부 확인
        if (!emailService.isEmailVerified(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 이메일로 회원 조회
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 소셜 로그인 회원은 비밀번호 변경 불가
        if (member.getPassword() == null) {
            throw new BusinessException(ErrorCode.PASSWORD_CHANGE_DENIED);
        }

        // 새 비밀번호 암호화 및 저장
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        member.updatePassword(encodedPassword);

        // 사용된 이메일 인증 데이터 삭제 (재사용 방지)
        emailService.deleteVerificationData(request.getEmail());
    }

    // 회원 탈퇴 (소프트 삭제)
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String deletedSuffix = "_deleted_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + memberId;
        member.markAsDeleted(deletedSuffix);
        member.changeStatus(MemberStatus.INACTIVE);
    }

    // 이메일 중복 확인
    public boolean isEmailDuplicate(String email) {
        return memberRepository.existsByEmail(email);
    }

    // 닉네임 중복 확인
    public boolean isNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname) ;
    }

    // 투표 알림 설정 변경
    @Transactional
    public void updateVoteNotificationSetting(Long memberId, Boolean enabled) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateVoteNotificationSetting(enabled);
    }
}
