package com.abdecd.moebackend.business.common.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "moe")
@Data
public class MoeProperties {
    private Integer captchaTtlSeconds;
    private String videoBasePath;
    private String defaultVideoPath;
    private String biliSession;
    private String bvUrl;
    private String proxyPrefix;
    private String bvProxyPrefix;
}
