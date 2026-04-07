package com.restartpoint.infra.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[Re:Start Point] 이메일 인증 코드");
            helper.setText(buildVerificationEmailContent(code), true);

            mailSender.send(message);
            log.info("인증 이메일 발송 완료: {}", to);
        } catch (MessagingException e) {
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
