package com.mechuragi.mechuragi_server.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class PerformanceLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 응답 전송 시작 시각 기록 (컨트롤러 진입 직전)
        request.setAttribute("responseSendStart", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long sendStart = (Long) request.getAttribute("responseSendStart");
        if (sendStart != null) {
            long sendMs = System.currentTimeMillis() - sendStart;
            log.info("[성능] Main→Client 응답 전송 완료: {}ms", sendMs);
        }
    }
}
