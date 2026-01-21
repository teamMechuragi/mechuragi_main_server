package com.mechuragi.mechuragi_server.domain.member.service;

import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import com.mechuragi.mechuragi_server.domain.member.dto.SignupRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdateMemberRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdatePasswordRequest;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.entity.type.MemberStatus;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.member.service.mapper.MemberMapper;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
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

    // 일반 회원가입
    @Transactional
    public MemberResponse signup(SignupRequest request) {

        // 회원 엔티티 생성 (Mapper 사용)
        Member member = memberMapper.toEntity(request);

        // 회원 저장
        Member savedMember = memberRepository.save(member);

        log.info("회원가입 완료: email={}, nickname={}", request.getEmail(), savedMember.getNickname());

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

    // 프로필 수정 (닉네임 및 프로필 사진 업데이트)
    @Transactional
    public MemberResponse updateMember(Long memberId, UpdateMemberRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 닉네임 중복 체크
        if (request.getNickname() != null && !request.getNickname().equals(member.getNickname())) {
            if (memberRepository.existsByNickname(request.getNickname())) {
                throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
            }
        }

        member.updateProfile(request.getNickname(), request.getProfileImageUrl());
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

    // 회원 탈퇴 (소프트 삭제)
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String deletedSuffix = "_deleted_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
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
