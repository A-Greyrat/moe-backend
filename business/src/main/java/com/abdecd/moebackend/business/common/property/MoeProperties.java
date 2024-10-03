package com.abdecd.moebackend.business.common.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "moe")
@Data
public class MoeProperties {
    /**
     * 图片验证码过期时间
     */
    private Integer captchaTtlSeconds;
    /**
     * 视频基础路径url，如 https://resource.xx
     */
    private String videoBasePath;
    /**
     * 默认视频url，如 https://resource.xx/video/default.mp4
     */
    private String defaultVideoPath;
    /**
     * SESSDATA
     */
    private String biliSession;
    /**
     * 获取bv视频链接的接口url
     */
    private String bvUrl;
    /**
     * 视频反代url前缀
     */
    private String proxyPrefix;
    /**
     * 视频反代url前缀
     */
    private String bvProxyPrefix;
    /**
     * 详见 {@link com.abdecd.moebackend.business.dao.dataencrypt.EncryptStrHandler}
     */
    public String encryptStrAesKey;
    // moe.local-file-service 详见 service/fileservice/LocalFileServiceImpl
}
