package com.mechuragi.mechuragi_server.auth.service;

import com.mechuragi.mechuragi_server.auth.entity.EmailVerification;
import com.mechuragi.mechuragi_server.auth.repository.EmailVerificationRepository;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    @Value("${cloud.aws.ses.from-email}")
    private String fromEmail;

    /**
     * 이메일 인증 메일 발송 (회원가입 전)
     */
    @Transactional
    public void sendVerificationEmail(String email) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

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
        try {
            log.info("이메일 발송 시도: email={}, code={}, fromEmail={}", email, verificationCode, fromEmail);
            sendEmail(email, verificationCode);
            log.info("이메일 인증 메일 발송 완료: email={}, code={}", email, verificationCode);
        } catch (Exception e) {
            log.error("이메일 발송 실패: email={}, error={}", email, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
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
                .orElseThrow(() -> new IllegalArgumentException("인증 코드가 일치하지 않습니다."));

        // 만료 여부 확인
        if (emailVerification.isExpired()) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다.");
        }

        // 이미 인증 완료된 경우
        if (emailVerification.getVerified()) {
            throw new IllegalArgumentException("이미 인증 완료된 이메일입니다.");
        }

        // 인증 완료 처리
        emailVerification.verify();

        log.info("이메일 인증 완료: email={}", email);
    }

    /**
     * 6자리 랜덤 인증 코드 생성
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }

    /**
     * AWS SES를 통한 이메일 발송
     */
    private void sendEmail(String toEmail, String verificationCode) {
        String subject = "[메추라기] 이메일 인증 코드";
        String htmlBody = buildEmailHtml(verificationCode);
        String textBody = String.format("이메일 인증 코드: %s\n\n이 코드는 30분간 유효합니다.", verificationCode);

        try {
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
        } catch (SesException e) {
            log.error("AWS SES 이메일 발송 실패: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    /**
     * 이메일 HTML 템플릿 생성
     */
    private String buildEmailHtml(String verificationCode) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background-color: #f9f9f9; }
                        .code { font-size: 32px; font-weight: bold; color: #4CAF50; text-align: center; padding: 20px; background-color: white; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>이메일 인증</h1>
                        </div>
                        <div class="content">
                            <p>안녕하세요, 메추라기입니다.</p>
                            <p>아래 인증 코드를 입력하여 이메일 인증을 완료해주세요.</p>
                            <div class="code">%s</div>
                            <p>이 코드는 <strong>30분간 유효</strong>합니다.</p>
                            <p>본인이 요청하지 않은 경우, 이 이메일을 무시하셔도 됩니다.</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 메추라기. All rights reserved.</p>
                            <p>이 이메일은 발신 전용입니다.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, verificationCode);
    }
}
