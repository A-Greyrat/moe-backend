package com.abdecd.moebackend.business.onstartup;

import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.mapper.VideoMapper;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RedisDataLoader implements ApplicationRunner {
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;

    @Override
    public void run(ApplicationArguments args) {
        var list = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .select(Video::getId)
        );
        for (var video : list) {
            videoGroupServiceBase.getVideoGroupInfo(video.getId());
        }
    }
}
