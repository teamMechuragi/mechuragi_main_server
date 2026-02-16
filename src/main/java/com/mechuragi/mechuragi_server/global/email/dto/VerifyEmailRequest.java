package com.mechuragi.mechuragi_server.global.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "인증 코드는 필수입니다.")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자입니다.")
    private String verificationCode;
}
