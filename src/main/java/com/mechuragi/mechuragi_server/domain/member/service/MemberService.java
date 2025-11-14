package com.mechuragi.mechuragi_server.domain.member.service;

import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import com.mechuragi.mechuragi_server.domain.member.dto.SignupRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdateMemberRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdatePasswordRequest;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.entity.type.MemberStatus;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.member.service.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;

    // 회원가입
    @Transactional
    public MemberResponse signup(SignupRequest request) {

        // 회원 엔티티 생성 (Mapper 사용)
        Member member = memberMapper.toEntity(request);

        // 회원 저장 (ID 발급을 위해)
        Member savedMember = memberRepository.save(member);

        // 닉네임 + ID 조합으로 최종 닉네임 업데이트 (예: "행복한곰1")
        savedMember.appendIdToNickname(request.getNickname());

        log.info("회원가입 완료: email={}, nickname={}", request.getEmail(), savedMember.getNickname());

        return memberMapper.toDto(savedMember);
    }

    // 회원 조회 (ID)
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return memberMapper.toDto(member);
    }

    // 회원 조회 (이메일)
    public MemberResponse getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return memberMapper.toDto(member);
    }

    // 회원 정보 수정
    @Transactional
    public MemberResponse updateMember(Long memberId, UpdateMemberRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 닉네임 중복 체크
        if (request.getNickname() != null && !request.getNickname().equals(member.getNickname())) {
            if (memberRepository.existsByNickname(request.getNickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
        }

        member.updateProfile(request.getNickname(), request.getProfileImageUrl());
        return memberMapper.toDto(member);
    }

    // 비밀번호 변경
    @Transactional
    public void updatePassword(Long memberId, UpdatePasswordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 소셜 로그인 회원은 비밀번호 변경 불가
        if (member.getPassword() == null) {
            throw new IllegalArgumentException("소셜 로그인 회원은 비밀번호를 변경할 수 없습니다.");
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 암호화 및 저장
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        member.updatePassword(encodedPassword);
    }

    // 회원 탈퇴 (소프트 삭제)
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        member.changeStatus(MemberStatus.INACTIVE);
    }

    // 이메일 중복 확인
    public boolean isEmailDuplicate(String email) {
        return memberRepository.existsByEmail(email);
    }

    // 닉네임 중복 확인
    public boolean isNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    // 투표 알림 설정 변경
    @Transactional
    public void updateVoteNotificationSetting(Long memberId, Boolean enabled) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        member.updateVoteNotificationSetting(enabled);
    }
}
