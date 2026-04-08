package com.restartpoint.infra.mail;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final Resend resend;
    private final String fromEmail;

    public EmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email:onboarding@resend.dev}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Async
    public void sendVerificationCode(String to, String code) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("Re:Start Point <" + fromEmail + ">")
                    .to(to)
                    .subject("[Re:Start Point] 이메일 인증 코드")
                    .html(buildVerificationEmailContent(code))
                    .build();

            resend.emails().send(params);
            log.info("인증 이메일 발송 완료: {}", to);
        } catch (ResendException e) {
            log.error("인증 이메일 발송 실패: {}", to, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    private String buildVerificationEmailContent(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
                    .header { text-align: center; margin-bottom: 40px; }
                    .logo { font-size: 24px; font-weight: bold; color: #6366f1; }
                    .code-box {
                        background: #f8fafc;
                        border: 2px solid #e2e8f0;
                        border-radius: 12px;
                        padding: 30px;
                        text-align: center;
                        margin: 30px 0;
                    }
                    .code {
                        font-size: 36px;
                        font-weight: bold;
                        letter-spacing: 8px;
                        color: #1e293b;
                    }
                    .info { color: #64748b; font-size: 14px; line-height: 1.6; }
                    .warning { color: #ef4444; font-size: 13px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">Re:Start Point</div>
                    </div>
                    <p>안녕하세요,</p>
                    <p>Re:Start Point 회원가입을 위한 이메일 인증 코드입니다.</p>
                    <div class="code-box">
                        <div class="code">%s</div>
                    </div>
                    <p class="info">
                        이 인증 코드는 <strong>10분간</strong> 유효합니다.<br>
                        본인이 요청하지 않은 경우 이 이메일을 무시해 주세요.
                    </p>
                    <p class="warning">
                        인증 코드를 타인과 공유하지 마세요.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
}
