package com.abdecd.moebackend.business.service;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class RecommendService {
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;
    @Autowired
    private VideoGroupMapper videoGroupMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ElasticSearchService elasticSearchService;

    public List<VideoGroupWithDataVO> getCarousel() {
        var self = SpringContextUtil.getBean(getClass());
        var ids = self.getCarouselIds();
        return new ArrayList<>(ids.stream()
                .map(id -> videoGroupServiceBase.getVideoGroupWithData(id))
                .filter(Objects::nonNull)
                .toList()
        );
    }

    public List<Long> getCarouselIds() {
        var idsStr = stringRedisTemplate.opsForValue().get(RedisConstant.RECOMMEND_CAROUSEL);
        if (idsStr == null || idsStr.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(
                Arrays.stream(idsStr.split(";")).map(Long::parseLong).toList()
        );
    }

    public void setCarouselIds(long[] ids) {
        if (ids.length > 20) throw new BaseException(MessageConstant.CAROUSEL_SIZE_LIMIT);
        stringRedisTemplate.opsForValue().set(RedisConstant.RECOMMEND_CAROUSEL, String.join(";", Arrays.stream(ids).mapToObj(String::valueOf).toArray(String[]::new)));
    }

    public void addCarouselIds(int index, long[] ids) {
        var old = getCarouselIds();
        if (old.size() + ids.length > 20) throw new BaseException(MessageConstant.CAROUSEL_SIZE_LIMIT);
        for (long id : ids) if (old.contains(id)) throw new BaseException(MessageConstant.CAROUSEL_EXIST_LIMIT);
        if (index < old.size()) {
            old.addAll(index, Arrays.stream(ids).boxed().toList());
        } else old.addAll(Arrays.stream(ids).boxed().toList());
        stringRedisTemplate.opsForValue().set(RedisConstant.RECOMMEND_CAROUSEL, String.join(";", old.stream().map(String::valueOf).toArray(String[]::new)));
    }

    public void deleteCarouselIds(long[] ids) {
        var old = getCarouselIds();
        old.removeAll(Arrays.stream(ids).boxed().toList());
        stringRedisTemplate.opsForValue().set(RedisConstant.RECOMMEND_CAROUSEL, String.join(";", old.stream().map(String::valueOf).toArray(String[]::new)));
    }

    public List<VideoGroupWithDataVO> getRecommend(int num) {
        var ids = videoGroupMapper.selectList(new LambdaQueryWrapper<VideoGroup>()
                .select(VideoGroup::getId)
                .eq(VideoGroup::getVideoGroupStatus, VideoGroup.Status.ENABLE)
                .last("order by RAND() limit " + num)
        );
        if (ids.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(ids.stream()
                .map(id -> videoGroupServiceBase.getVideoGroupWithData(id.getId()))
                .filter(Objects::nonNull)
                .toList()
        );
    }

    public List<VideoGroupWithDataVO> getRelated(Long videoGroupId, int num) {
        var vg = videoGroupServiceBase.getVideoGroupInfo(videoGroupId);
        if (vg == null) return new ArrayList<>();
        var result = elasticSearchService.searchRelated(vg.getTags().replaceAll(";"," "), 1, num + 1);
        if (result.getTotal() == 0) return new ArrayList<>();
        return new ArrayList<>(result.getRecords().stream()
                .filter(r -> !Objects.equals(r.getVideoGroupVO().getId(), videoGroupId))
                .toList()
        );
    }
}
