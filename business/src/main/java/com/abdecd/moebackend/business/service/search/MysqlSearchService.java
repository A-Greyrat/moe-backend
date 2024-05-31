package com.abdecd.moebackend.business.service.search;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.result.PageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;

@ConditionalOnMissingBean(ElasticSearchService.class)
@Service
public class MysqlSearchService implements SearchService {
    @Autowired
    private VideoGroupMapper videoGroupMapper;

    @Override
    public void initData(List<VideoGroupVO> videoGroups) {
    }

    @Override
    public void saveSearchEntity(VideoGroupVO videoGroupVO) {
    }

    @Override
    public void deleteSearchEntity(Long id) {
    }

    @Override
    public PageVO<VideoGroupWithDataVO> search(String keyword, Byte type, Integer page, Integer pageSize) {
        var pageObj = new Page<VideoGroup>(page, pageSize);
        var ids = videoGroupMapper.selectPage(pageObj, new LambdaQueryWrapper<VideoGroup>()
            .select(VideoGroup::getId, VideoGroup::getTitle)
            .eq(type != null, VideoGroup::getType, type)
            .like(VideoGroup::getTitle, keyword)
        );
        var videoGroupServiceBase = SpringContextUtil.getBean(VideoGroupServiceBase.class);
        return new PageVO<VideoGroupWithDataVO>()
            .setTotal((int) ids.getTotal())
            .setRecords(ids.getRecords().stream().parallel()
                .map(id -> videoGroupServiceBase.getVideoGroupWithData(id.getId()))
                .toList()
            );
    }

    @Override
    public PageVO<VideoGroupWithDataVO> searchRelated(String keyword, Integer page, Integer pageSize) {
        var pageObj = new Page<VideoGroup>(page, pageSize);
        var ids = videoGroupMapper.selectPage(pageObj, new LambdaQueryWrapper<VideoGroup>()
            .select(VideoGroup::getId, VideoGroup::getTitle)
            .last("order by RAND()")
        );
        var videoGroupServiceBase = SpringContextUtil.getBean(VideoGroupServiceBase.class);
        return new PageVO<VideoGroupWithDataVO>()
            .setTotal((int) ids.getTotal())
            .setRecords(ids.getRecords().stream().parallel()
                .map(id -> videoGroupServiceBase.getVideoGroupWithData(id.getId()))
                .toList()
            );
    }

    @Override
    public List<String> getSearchSuggestions(String keyword, Integer num) {
        var objs = videoGroupMapper.selectList(new LambdaQueryWrapper<VideoGroup>()
            .select(VideoGroup::getId, VideoGroup::getTitle)
            .like(VideoGroup::getTitle, keyword)
            .last("limit " + num)
        );
        return objs.stream().map(VideoGroup::getTitle).toList();
    }
}
