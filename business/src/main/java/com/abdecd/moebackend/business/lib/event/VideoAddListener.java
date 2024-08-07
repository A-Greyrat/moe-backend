package com.abdecd.moebackend.business.lib.event;

import com.abdecd.moebackend.business.dao.mapper.DanmakuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class VideoAddListener implements ApplicationListener<VideoAddEvent> {

    @Autowired
    private DanmakuMapper danmakuMapper;

    @Override
    public void onApplicationEvent(VideoAddEvent event) {
        refreshDanmakuPartition(event.getVideo().getId());
    }

    protected void refreshDanmakuPartition(Long videoId) {
        danmakuMapper.refreshPartition(videoId);
    }
}