package com.abdecd.moebackend.business.lib;

import cn.hutool.core.util.URLUtil;
import com.abdecd.moebackend.business.common.property.MoeProperties;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class BiliParser {
    @Autowired
    private OkHttpClient okHttpClient;
    @Autowired
    private MoeProperties moeProperties;
    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, String> bvQualityMap = Map.of("360p","16", "480p","32", "720p","64", "1080p","80");

    @Cacheable(cacheNames = RedisConstant.BILI_PARSER_BV, key = "#bvid + ':' + #quality + ':' + #p")
    public String parseBV(String bvid, String quality, String p) throws IOException {
        log.info("parseBV: bvid={}, quality={}, p={}", bvid, quality, p);
        try (var resp = okHttpClient.newCall(new Request.Builder()
            .url(HttpUrl.get(moeProperties.getBvUrl()).newBuilder()
                .addQueryParameter("bvid", bvid)
                .addQueryParameter("SESSDATA", moeProperties.getBiliSession())
                .addQueryParameter("qn", bvQualityMap.get(quality))
                .addQueryParameter("p", p)
                .build()
            )
            .build()
        ).execute()) {
            if (resp.code() != 200) throw new IOException();
            if (resp.body() != null) {
                var json = objectMapper.readTree(resp.body().string());
                var url = json.get("data")
                    .get("durl")
                    .get(0)
                    .get("url");
                if (moeProperties.getBvProxyPrefix() != null) {
                    return moeProperties.getBvProxyPrefix() + url.textValue().substring(url.textValue().indexOf("/upgcxcode"));
                } else {
                    return moeProperties.getProxyPrefix() + URLUtil.encodeAll(url.textValue());
                }
            }
        }
        return null;
    }
}
