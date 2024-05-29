package com.abdecd.moebackend.business.onstartup;

import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoMapper;
import com.abdecd.moebackend.business.service.danmaku.DanmakuService;
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
    private VideoGroupMapper videoGroupMapper;
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;
    @Autowired
    private DanmakuService danmakuService;

    @Override
    public void run(ApplicationArguments args) {
        var list = videoGroupMapper.selectList(new LambdaQueryWrapper<VideoGroup>()
            .select(VideoGroup::getId)
        );
        for (var vg : list) {
            videoGroupServiceBase.getVideoGroupInfo(vg.getId());
        }
        var list2 = videoMapper.selectList(new LambdaQueryWrapper<Video>()
            .select(Video::getId)
        );
        for (var v : list2) {
            danmakuService.getDanmakuCount(v.getId());
        }
    }
}
