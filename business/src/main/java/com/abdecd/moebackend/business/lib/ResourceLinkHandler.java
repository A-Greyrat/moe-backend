package com.abdecd.moebackend.business.lib;

import com.abdecd.moebackend.business.common.property.MoeProperties;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourceLinkHandler {
    @Autowired
    MoeProperties moeProperties;

    public String getVideoLink(String path) {
        return moeProperties.getVideoBasePath() + "/" + path;
    }

    /**
     * 返回值不以 "/" 开头 错误返回空字符串
     * @param link 如 tmp/user10000/xx.mp4
     * @return 如 tmp/user10000/xx.mp4
     */
    public String getRawPathFromTmpVideoLink(String link) {
        if (!link.startsWith("tmp/user"+ UserContext.getUserId()+"/")) return "";
        return link;
    }
}
