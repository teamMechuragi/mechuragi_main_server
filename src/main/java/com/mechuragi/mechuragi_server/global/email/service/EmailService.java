package com.mechuragi.mechuragi_server.global.email.service;

import com.mechuragi.mechuragi_server.global.email.entity.EmailVerification;
import com.mechuragi.mechuragi_server.global.email.repository.EmailVerificationRepository;
import com.mechuragi.mechuragi_server.global.email.template.EmailTemplate;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailService {

    private final SesClient sesClient;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailTemplate emailTemplate;

    @Value("${cloud.aws.ses.from-email}")
    private String fromEmail;

    /**
     * 이메일 인증 메일 발송 (회원가입 전)
     */
    @Transactional
    public void sendVerificationEmail(String email) {
        // 인증 코드 생성
        String verificationCode = generateVerificationCode();

        // 기존 인증 정보가 있으면 업데이트, 없으면 새로 생성
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30); // 30분 유효
        EmailVerification emailVerification = emailVerificationRepository.findByEmail(email)
                .map(verification -> {
                    verification.updateCode(verificationCode, expiresAt);
                    return verification;
                })
                .orElseGet(() -> {
                    EmailVerification newVerification = EmailVerification.builder()
                            .email(email)
                            .verificationCode(verificationCode)
                            .expiresAt(expiresAt)
                            .verified(false)
                            .build();
                    return emailVerificationRepository.save(newVerification);
                });

        // 이메일 발송
        String subject = "[메추라기] 이메일 인증 코드";
        String htmlBody = emailTemplate.buildVerificationEmail(verificationCode);
        String textBody = String.format("이메일 인증 코드: %s\n\n이 코드는 30분간 유효합니다.", verificationCode);

        try {
            log.info("이메일 발송 시도: email={}, code={}, fromEmail={}", email, verificationCode, fromEmail);
            send(email, subject, htmlBody, textBody);
            log.info("이메일 인증 메일 발송 완료: email={}, code={}", email, verificationCode);
        } catch (SesException e) {
            log.error("AWS SES 이메일 발송 실패: {}", e.awsErrorDetails().errorMessage());
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        } catch (Exception e) {
            log.error("이메일 발송 실패: email={}, error={}", email, e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /**
     * 이메일 인증 코드 확인 (회원가입 전)
     */
    @Transactional
    public void verifyEmail(String email, String verificationCode) {
        // 인증 정보 조회
        EmailVerification emailVerification = emailVerificationRepository
                .findByEmailAndVerificationCode(email, verificationCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH));

        // 만료 여부 확인
        if (emailVerification.isExpired()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
        }

        // 이미 인증 완료된 경우
        if (emailVerification.getVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // 인증 완료 처리
        emailVerification.verify();

        log.info("이메일 인증 완료: email={}", email);
    }

    /**
     * 회원가입 환영 메일 발송
     */
    public void sendWelcomeEmail(String email, String nickname) {
        String subject = "[메추라기] 회원가입을 환영합니다!";
        String htmlBody = emailTemplate.buildWelcomeEmail(nickname);
        String textBody = String.format("%s님, 메추라기 회원가입을 환영합니다!", nickname);

        try {
            send(email, subject, htmlBody, textBody);
            log.info("회원가입 환영 메일 발송 완료: email={}", email);
        } catch (Exception e) {
            log.warn("회원가입 환영 메일 발송 실패 (회원가입은 정상 처리): email={}, error={}", email, e.getMessage());
        }
    }

    /**
     * AWS SES를 통한 이메일 발송
     */
    private void send(String toEmail, String subject, String htmlBody, String textBody) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder()
                        .toAddresses(toEmail)
                        .build())
                .message(Message.builder()
                        .subject(Content.builder()
                                .charset("UTF-8")
                                .data(subject)
                                .build())
                        .body(Body.builder()
                                .html(Content.builder()
                                        .charset("UTF-8")
                                        .data(htmlBody)
                                        .build())
                                .text(Content.builder()
                                        .charset("UTF-8")
                                        .data(textBody)
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }

    /**
     * 6자리 랜덤 인증 코드 생성
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }

}
