package com.abdecd.moebackend.business.service.search;

import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.common.result.PageVO;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    void initData(List<VideoGroupVO> videoGroups) throws IOException;

    @SneakyThrows
    void saveSearchEntity(VideoGroupVO videoGroupVO);

    @SneakyThrows
    void deleteSearchEntity(Long id);

    PageVO<VideoGroupWithDataVO> search(String keyword, Byte type, Integer page, Integer pageSize);

    PageVO<VideoGroupWithDataVO> searchRelated(String keyword, Integer page, Integer pageSize);

    @SneakyThrows
    List<String> getSearchSuggestions(String keyword, Integer num);
}
