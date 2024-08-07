package com.abdecd.moebackend.business.lib.event;

import com.abdecd.moebackend.business.dao.entity.Video;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VideoAddEvent extends ApplicationEvent {
    private final Video video;

    public VideoAddEvent(Object source, Video video) {
        super(source);
        this.video = video;
    }
}