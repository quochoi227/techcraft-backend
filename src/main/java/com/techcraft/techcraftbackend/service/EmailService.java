package com.techcraft.techcraftbackend.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.techcraft.techcraftbackend.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final Resend resend;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${resend.from-email-name:PC Shop Support}")
    private String fromEmailName;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    public void sendVerificationEmail(String toEmail, String recipientName, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        String directApiUrl = backendUrl + "/api/auth/verify-email?token=" + token;

        String formattedFrom = (fromEmailName != null && !fromEmailName.isBlank())
                ? String.format("%s <%s>", fromEmailName.replace("'", "").trim(), fromEmail)
                : fromEmail;

        String subject = "[PC Shop] Xác thực địa chỉ email của bạn";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;">
                <h2 style="color: #2563eb; text-align: center;">Chào mừng bạn đến với PC Shop!</h2>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Cảm ơn bạn đã đăng ký tài khoản tại cửa hàng linh kiện máy tính PC Shop.</p>
                <p>Để hoàn tất quá trình đăng ký và kích hoạt tài khoản, vui lòng nhấn vào nút bên dưới để xác thực địa chỉ email:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">
                        Xác thực tài khoản
                    </a>
                </div>
                <p style="color: #64748b; font-size: 14px;">Hoặc bạn có thể truy cập trực tiếp đường link sau:</p>
                <p style="word-break: break-all; font-size: 13px; color: #2563eb;"><a href="%s">%s</a></p>
                <p style="font-size: 13px; color: #64748b;">(Nếu liên kết frontend chưa mở được, bạn có thể click xác thực trực tiếp qua máy chủ: <a href="%s">%s</a>)</p>
                <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;" />
                <p style="color: #94a3b8; font-size: 12px; text-align: center;">
                    Liên kết xác thực này sẽ hết hạn sau 24 giờ.<br/>
                    Nếu bạn không thực hiện đăng ký tài khoản này, vui lòng bỏ qua email.
                </p>
            </div>
            """.formatted(
                recipientName != null ? recipientName : "Quý khách",
                verificationUrl,
                verificationUrl,
                verificationUrl,
                directApiUrl,
                directApiUrl
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(formattedFrom)
                .to(toEmail)
                .subject(subject)
                .html(htmlContent)
                .build();

        try {
            log.info("Sending verification email to: {}", toEmail);
            CreateEmailResponse response = resend.emails().send(params);
            log.info("Verification email sent successfully with id: {}", response.getId());
        } catch (ResendException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            throw new AppException("Không thể gửi email xác thực. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
