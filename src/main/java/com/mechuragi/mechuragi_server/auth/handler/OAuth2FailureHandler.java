package com.mechuragi.mechuragi_server.auth.handler;

import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 시 에러 코드를 쿼리 파라미터로 담아 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 로그인 실패: {}", exception.getMessage());

        String errorCode = "OAUTH2_ERROR";
        String message = "소셜 로그인 처리 중 오류가 발생했습니다.";

        // InternalAuthenticationServiceException 내부에 BusinessException이 래핑되어 있음
        Throwable cause = exception.getCause();
        if (cause instanceof BusinessException businessException) {
            errorCode = businessException.getErrorCode().getCode();
            message = businessException.getErrorCode().getMessage();
        }

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", errorCode)
                .queryParam("message", message)
                .build()
                .toUriString();

        log.info("OAuth2 실패 후 프론트엔드로 리다이렉트: {}", targetUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
