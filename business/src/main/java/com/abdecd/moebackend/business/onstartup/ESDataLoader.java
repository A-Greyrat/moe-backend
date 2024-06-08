package com.abdecd.moebackend.business.onstartup;

import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupVO;
import com.abdecd.moebackend.business.service.search.SearchService;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@ConditionalOnProperty(prefix = "spring.data.elasticsearch", name = "url")
@Component
public class ESDataLoader implements ApplicationRunner {
    @Autowired
    private SearchService searchService;
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        var videoGroupVOs = videoGroupServiceBase.listAllAvailableVideoGroupId()
                .stream().map(id -> videoGroupServiceBase.getVideoGroupInfo(id)).toList();
        loadSearchEntity(videoGroupVOs);
        System.gc();
    }

    public void loadSearchEntity(List<VideoGroupVO> videoGroupVOs) throws IOException {
        searchService.initData(videoGroupVOs);
    }
}
