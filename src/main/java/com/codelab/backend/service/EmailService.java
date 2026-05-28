package com.codelab.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${application.frontend-url}")
    private String frontendUrl;

    // @Async means this runs in a background thread
    // Registration won't hang waiting for Gmail to respond
    @Async
    public void sendVerificationEmail(String toEmail, String username, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // true = multipart (needed for HTML emails)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to CodeLab — Verify your email");
            helper.setText(buildVerificationEmailBody(username, token), true); // true = isHtml

            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            // We don't throw here — registration already succeeded
            // User can request a resend later (Phase 5 enhancement)
        }
    }

    private String buildVerificationEmailBody(String username, String token) {
        String verifyLink = frontendUrl + "/verify-email?token=" + token;

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: #ffffff;
                                     border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background: #4f46e5; padding: 32px; text-align: center; }
                        .header h1 { color: #ffffff; margin: 0; font-size: 24px; }
                        .body { padding: 32px; color: #374151; }
                        .body p { line-height: 1.6; margin: 0 0 16px; }
                        .btn { display: inline-block; background: #4f46e5; color: #ffffff;
                               text-decoration: none; padding: 14px 32px; border-radius: 6px;
                               font-weight: bold; font-size: 16px; margin: 8px 0; }
                        .footer { background: #f9fafb; padding: 20px 32px;
                                  text-align: center; color: #9ca3af; font-size: 12px; }
                        .token-box { background: #f3f4f6; border-radius: 6px; padding: 12px 16px;
                                     font-family: monospace; font-size: 13px;
                                     word-break: break-all; color: #6b7280; margin-top: 16px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Welcome to CodeLab!</h1>
                        </div>
                        <div class="body">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>Thanks for registering. Please verify your email address
                               by clicking the button below. This link expires in
                               <strong>24 hours</strong>.</p>
                            <p style="text-align:center; margin: 32px 0;">
                                <a href="%s" class="btn">Verify Email Address</a>
                            </p>
                            <p>If the button doesn't work, copy and paste this link:</p>
                            <div class="token-box">%s</div>
                            <p style="margin-top:24px; font-size:13px; color:#9ca3af;">
                                If you didn't create an account, you can safely ignore this email.
                            </p>
                        </div>
                        <div class="footer">
                            &copy; 2025 CodeLab. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, verifyLink, verifyLink);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail,
                                       String username,
                                       String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset your CodeLab password");
            helper.setText(buildPasswordResetEmailBody(username, token), true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildPasswordResetEmailBody(String username, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background: #f4f4f4; }
                    .container { max-width: 600px; margin: 40px auto;
                                 background: #fff; border-radius: 8px;
                                 overflow: hidden;
                                 box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                    .header { background: #4f46e5; padding: 32px;
                              text-align: center; }
                    .header h1 { color: #fff; margin: 0; font-size: 24px; }
                    .body { padding: 32px; color: #374151; }
                    .body p { line-height: 1.6; margin: 0 0 16px; }
                    .btn { display: inline-block; background: #4f46e5;
                           color: #fff; text-decoration: none;
                           padding: 14px 32px; border-radius: 6px;
                           font-weight: bold; }
                    .footer { background: #f9fafb; padding: 20px 32px;
                              text-align: center; color: #9ca3af;
                              font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1>Password Reset</h1></div>
                    <div class="body">
                        <p>Hi <strong>%s</strong>,</p>
                        <p>We received a request to reset your password.
                           Click below — this link expires in
                           <strong>1 hour</strong>.</p>
                        <p style="text-align:center; margin: 32px 0;">
                            <a href="%s" class="btn">Reset Password</a>
                        </p>
                        <p style="font-size:13px; color:#9ca3af;">
                            If you didn't request this, ignore this email.
                            Your password won't change.
                        </p>
                    </div>
                    <div class="footer">&copy; 2025 CodeLab</div>
                </div>
            </body>
            </html>
            """.formatted(username, resetLink);
    }
}
