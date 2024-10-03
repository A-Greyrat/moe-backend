package com.abdecd.moebackend.business.service.common;

import com.abdecd.moebackend.business.common.property.MoeProperties;
import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CommonServiceImpl implements CommonService {
    @Autowired
    DefaultKaptcha captchaProducer;
    @Autowired
    StringRedisTemplate redisTemplate;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;
    @Resource
    MoeProperties moeProperties;

    private static final String VERIFY_CODE_PREFIX = RedisConstant.VERIFY_CODE_PREFIX;
    private static final String EMAIL_VERIFY_CODE_PREFIX = RedisConstant.EMAIL_VERIFY_CODE_PREFIX;

    @Override
    public Pair<String, byte[]> generateCaptcha() {
        byte[] captchaOutputStream;
        var verifyCodeId = UUID.randomUUID().toString();
        try (
                ByteArrayOutputStream imgOutputStream = new ByteArrayOutputStream()
        ) {
            String verifyCode = captchaProducer.createText();
            // 存下答案
            redisTemplate.opsForValue().set(VERIFY_CODE_PREFIX + verifyCodeId, verifyCode, moeProperties.getCaptchaTtlSeconds(), TimeUnit.SECONDS);

            // 生成图片
            BufferedImage challenge = captchaProducer.createImage(verifyCode);
            ImageIO.write(challenge, "jpg", imgOutputStream);
            captchaOutputStream = imgOutputStream.toByteArray();
        } catch (IllegalArgumentException | IOException e) {
            throw new BaseException(MessageConstant.UNKNOWN_ERROR);
        }
        return Pair.of(verifyCodeId, captchaOutputStream);
    }

    @Override
    public void verifyCaptcha(String uuid, String captcha) {
        var key = VERIFY_CODE_PREFIX + uuid;
        verifyCodeInRedis(key, captcha);
    }

    @Override
    public void sendCodeToVerifyEmail(String email) {
        var key = EMAIL_VERIFY_CODE_PREFIX + email;
        // 生成验证码
        StringBuilder code = new StringBuilder(new Random().nextInt(0, 1000000) + "");
        while (code.length() < 6) code.insert(0, "0");
        // 存入redis
        redisTemplate.opsForValue().set(key, code.toString(), moeProperties.getCaptchaTtlSeconds(), TimeUnit.SECONDS);
        // 发送邮件
        var message = mailSender.createMimeMessage();
        try {
            var helper = new MimeMessageHelper(message, true);
            helper.setFrom("\"moe\" <" + from + ">");
            helper.setTo(email);
            helper.setSubject("邮箱验证");
            helper.setText("验证码：" + code + "，有效期：" + moeProperties.getCaptchaTtlSeconds() / 60 + "分钟。");
            mailSender.send(message);
        } catch (Exception e) {
            throw new BaseException(MessageConstant.EMAIL_SEND_FAIL);
        }
    }

    @Override
    public void verifyEmail(String email, String code) {
        var key = EMAIL_VERIFY_CODE_PREFIX + email;
        verifyCodeInRedis(key, code);
    }

    private void verifyCodeInRedis(String key, String value) {
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key)))
                throw new BaseException(MessageConstant.CAPTCHA_EXPIRED);
            if (!Objects.equals(redisTemplate.opsForValue().get(key), value))
                throw new BaseException(MessageConstant.CAPTCHA_ERROR);
        } finally {
            redisTemplate.delete(key);
        }
    }
}
