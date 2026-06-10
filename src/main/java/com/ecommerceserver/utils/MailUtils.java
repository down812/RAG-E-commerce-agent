package com.ecommerceserver.utils;

import com.ecommerceserver.constants.MessageConstant;
import com.ecommerceserver.constants.RedisConstant;
import com.ecommerceserver.exception.CodeException;
import com.ecommerceserver.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MailUtils {
    
    @Value("${spring.mail.host:smtp.qq.com}")
    private String host;
    
    @Value("${spring.mail.port:465}")
    private String port;
    
    @Value("${spring.mail.username:}")
    private String username;
    
    @Value("${spring.mail.password:}")
    private String password;

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 发送邮件验证码
     */
    public boolean sendEmailCode(String toEmail, String code) {
        //1.限制发送频率(1分钟内只能发1次)
        String limitKey = RedisConstant.EMAIL_LIMIT_KEY+toEmail;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            log.warn("邮件发送频率过高，收件人: {}", toEmail);
            throw new CodeException(Result.error(MessageConstant.CODE_SEND_FREQUENT));
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "jakarta.net.ssl.SSLSocketFactory");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("验证码 - 您的应用名称");
            
            String content = buildEmailContent(code);
            message.setContent(content, "text/html;charset=UTF-8");
            
            Transport.send(message);
            log.info("邮件发送成功，收件人: {}", toEmail);

            //验证码存入Redis，有效期5分钟
            String emailCodeKey = RedisConstant.EMAIL_CODE_KEY+toEmail;
            redisTemplate.opsForValue().set(emailCodeKey, code, 5, TimeUnit.MINUTES);
            //设置发送频率限制为1分钟
            redisTemplate.opsForValue().set(limitKey, "1", 1, TimeUnit.MINUTES);
            return true;
            
        } catch (Exception e) {
            log.error("邮件发送失败，收件人: {}, 错误: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String code) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <title>验证码</title>" +
                "</head>" +
                "<body>" +
                "    <div style=\"max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;\">" +
                "        <h2 style=\"color: #333;\">验证码通知</h2>" +
                "        <p>您的验证码是：</p>" +
                "        <div style=\"font-size: 24px; font-weight: bold; color: #1890ff; padding: 10px; background: #f5f5f5; text-align: center;\">" +
                "            " + code +
                "        </div>" +
                "        <p>验证码有效期为 <strong>5分钟</strong>，请尽快使用。</p>" +
                "        <p>如果这不是您的操作，请忽略此邮件。</p>" +
                "        <hr>" +
                "        <p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}