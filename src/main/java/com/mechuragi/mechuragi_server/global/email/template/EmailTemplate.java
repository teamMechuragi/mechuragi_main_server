package com.mechuragi.mechuragi_server.global.email.template;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplate {

    /**
     * 이메일 인증 HTML 템플릿
     */
    public String buildVerificationEmail(String verificationCode) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#f4f5f7; font-family:'Apple SD Gothic Neo','Noto Sans KR',Arial,sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7; padding:40px 20px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background-color:#3bdbb9; padding:32px 40px; text-align:center;">
                                            <h1 style="margin:0; font-size:22px; font-weight:700; color:#ffffff; letter-spacing:-0.3px;">메추라기</h1>
                                        </td>
                                    </tr>
                                    <!-- Body -->
                                    <tr>
                                        <td style="padding:40px 40px 32px;">
                                            <p style="margin:0 0 8px; font-size:18px; font-weight:700; color:#222222; letter-spacing:-0.3px;">이메일 인증</p>
                                            <p style="margin:0 0 28px; font-size:14px; color:#555555; line-height:1.6;">
                                                안녕하세요, 메추라기입니다.<br/>
                                                아래 인증 코드를 입력하여 이메일 인증을 완료해주세요.
                                            </p>
                                            <!-- Code Box -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="background-color:#f2f3f5; border-radius:12px; padding:24px; text-align:center;">
                                                        <span style="font-size:32px; font-weight:800; color:#222222; letter-spacing:8px; font-family:'Courier New',monospace;">%s</span>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin:24px 0 0; font-size:13px; color:#888888; line-height:1.6;">
                                                이 코드는 <span style="color:#3bdbb9; font-weight:600;">30분간 유효</span>합니다.<br/>
                                                본인이 요청하지 않은 경우, 이 이메일을 무시하셔도 됩니다.
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- Divider -->
                                    <tr>
                                        <td style="padding:0 40px;">
                                            <div style="border-top:1px solid #eeeeee;"></div>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding:20px 40px 28px; text-align:center;">
                                            <p style="margin:0; font-size:11px; color:#aaaaaa; line-height:1.6;">
                                                &copy; 2025 메추라기. All rights reserved.<br/>
                                                이 이메일은 발신 전용입니다.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, verificationCode);
    }

    /**
     * 회원가입 환영 HTML 템플릿
     */
    public String buildWelcomeEmail(String nickname) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#f4f5f7; font-family:'Apple SD Gothic Neo','Noto Sans KR',Arial,sans-serif;">
                    <table role="presentation" width="100%%%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7; padding:40px 20px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background-color:#3bdbb9; padding:32px 40px; text-align:center;">
                                            <h1 style="margin:0; font-size:22px; font-weight:700; color:#ffffff; letter-spacing:-0.3px;">메추라기</h1>
                                        </td>
                                    </tr>
                                    <!-- Body -->
                                    <tr>
                                        <td style="padding:40px 40px 32px;">
                                            <p style="margin:0 0 8px; font-size:18px; font-weight:700; color:#222222; letter-spacing:-0.3px;">환영합니다!</p>
                                            <p style="margin:0 0 28px; font-size:14px; color:#555555; line-height:1.6;">
                                                <span style="color:#3bdbb9; font-weight:600;">%s</span>님, 메추라기에 가입해주셔서 감사합니다.
                                            </p>
                                            <!-- Welcome Box -->
                                            <table role="presentation" width="100%%%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="background-color:#f2f3f5; border-radius:12px; padding:24px 28px;">
                                                        <p style="margin:0 0 12px; font-size:14px; font-weight:600; color:#222222;">메추라기에서 이런 것들을 할 수 있어요</p>
                                                        <p style="margin:0; font-size:13px; color:#555555; line-height:2;">
                                                            &#8226; 일상의 고민을 투표로 물어보기<br/>
                                                            &#8226; 다른 사람들의 투표에 참여하기<br/>
                                                            &#8226; 나만의 일기 작성하기
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin:24px 0 0; font-size:13px; color:#888888; line-height:1.6;">
                                                궁금한 점이 있으시면 언제든지 문의해주세요.
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- Divider -->
                                    <tr>
                                        <td style="padding:0 40px;">
                                            <div style="border-top:1px solid #eeeeee;"></div>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding:20px 40px 28px; text-align:center;">
                                            <p style="margin:0; font-size:11px; color:#aaaaaa; line-height:1.6;">
                                                &copy; 2025 메추라기. All rights reserved.<br/>
                                                이 이메일은 발신 전용입니다.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, nickname);
    }
}
