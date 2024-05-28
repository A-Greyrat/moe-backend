package com.abdecd.moebackend.business.service;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.BangumiVideoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.pojo.vo.bangumiindex.HotTags;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class BangumiIndexService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;
    @Autowired
    private VideoGroupMapper videoGroupMapper;
    @Autowired
    private BangumiVideoGroupMapper bangumiVideoGroupMapper;
    @Autowired
    private RedissonClient redissonClient;

    public enum BangumiIndexType {
        FAVORITE_CNT(0, "追番人数"),
        UPDATE_TIME(1, "更新时间"),
        WATCH_CNT(2, "播放数量");

        public final Byte TYPE;
        public final String NAME;
        BangumiIndexType(int type, String name) {
            this.TYPE = (byte) type;
            this.NAME = name;
        }
    }
    record BangumiIndexItem(Byte type, String name) {}

    public List<HotTags> getHotTags() {
        var list = new ArrayList<HotTags>(4);
        list.add(new HotTags()
                .setTitle("番剧索引")
                .setTags(List.of(
                        new BangumiIndexItem(BangumiIndexType.FAVORITE_CNT.TYPE, BangumiIndexType.FAVORITE_CNT.NAME),
                        new BangumiIndexItem(BangumiIndexType.UPDATE_TIME.TYPE, BangumiIndexType.UPDATE_TIME.NAME),
                        new BangumiIndexItem(BangumiIndexType.WATCH_CNT.TYPE, BangumiIndexType.WATCH_CNT.NAME)
                ))
        );
        list.add(new HotTags()
                .setTitle("类型风格")
                .setTags(List.of("原创", "小说改", "特摄", "漫画改", "游戏改", "布袋戏"))
        );
        list.add(new HotTags()
                .setTitle("首播时间")
                .setTags(List.of("2024", "2023", "2022", "2021", "2020", "2019"))
        );
        var self = SpringContextUtil.getBean(getClass());
        list.add(new HotTags()
                .setTitle("热门")
                .setTags(self.listHot(6)
                        .stream()
                        .map(videoGroupId -> {
                            var vo = videoGroupServiceBase.getVideoGroupInfo(videoGroupId);
                            return vo == null ? (Object) "" : vo.getTitle();
                        })
                        .toList()
                )
        );
        return list;
    }

    /**
     * 会自动判断视频组类型
     */
    public void recordHot(Long videoGroupId) {
        if (videoGroupServiceBase.getVideoGroupType(videoGroupId) == null) return;
        if (!Objects.equals(videoGroupServiceBase.getVideoGroupType(videoGroupId), VideoGroup.Type.ANIME_VIDEO_GROUP)) return;
        stringRedisTemplate.opsForZSet().incrementScore(RedisConstant.BANGUMI_INDEX_HOT, videoGroupId + "", 1);
        // 设置过期时间
        var exp = stringRedisTemplate.getExpire(RedisConstant.BANGUMI_INDEX_HOT);
        if (exp == null || exp < 0) {
            stringRedisTemplate.expire(RedisConstant.BANGUMI_INDEX_HOT, RedisConstant.BANGUMI_INDEX_HOT_RESET_TIME, TimeUnit.DAYS);
        }
    }

    public List<Long> listHot(int num) {
        var set = stringRedisTemplate.opsForZSet().reverseRange(RedisConstant.BANGUMI_INDEX_HOT, 0, num - 1);
        if (set == null) return new ArrayList<>();

        var arr = new ArrayList<>(set.stream().map(Long::parseLong).toList());
        if (arr.size() < num) {
            var randomList = videoGroupMapper.selectList(new LambdaQueryWrapper<VideoGroup>()
                    .select(VideoGroup::getId)
                    .eq(VideoGroup::getVideoGroupStatus, VideoGroup.Status.ENABLE)
                    .eq(VideoGroup::getType, VideoGroup.Type.ANIME_VIDEO_GROUP)
                    .notIn(!arr.isEmpty(), VideoGroup::getId, arr)
                    .last("order by rand() limit " + (num - arr.size()))
            );
            randomList.forEach(videoGroup -> arr.add(videoGroup.getId()));
        }
        return arr;
    }

    /**
     * 会自动判断视频组类型
     */
    public void recordFavorite(Long videoGroupId) {
        if (videoGroupServiceBase.getVideoGroupType(videoGroupId) == null) return;
        if (!Objects.equals(videoGroupServiceBase.getVideoGroupType(videoGroupId), VideoGroup.Type.ANIME_VIDEO_GROUP)) return;
        var lock = redissonClient.getLock(RedisConstant.BANGUMI_INDEX_FAVORITE_CNT_LOCK);
        lock.lock();
        try {
            stringRedisTemplate.opsForZSet().incrementScore(RedisConstant.BANGUMI_INDEX_FAVORITE_CNT, videoGroupId + "", 1);
        } finally {
            lock.unlock();
        }
    }
    /**
     * 会自动判断视频组类型
     */
    public void decreaseFavorite(Long videoGroupId) {
        if (videoGroupServiceBase.getVideoGroupType(videoGroupId) == null) return;
        if (!Objects.equals(videoGroupServiceBase.getVideoGroupType(videoGroupId), VideoGroup.Type.ANIME_VIDEO_GROUP)) return;
        var lock = redissonClient.getLock(RedisConstant.BANGUMI_INDEX_FAVORITE_CNT_LOCK);
        lock.lock();
        try {
            var ans = stringRedisTemplate.opsForZSet().incrementScore(RedisConstant.BANGUMI_INDEX_FAVORITE_CNT, videoGroupId + "", -1);
            if (ans != null && ans == 0)
                stringRedisTemplate.opsForZSet().remove(RedisConstant.BANGUMI_INDEX_FAVORITE_CNT, videoGroupId + "");
        } finally {
            lock.unlock();
        }
    }

    public List<Long> listFavorite(int num) {
        var set = stringRedisTemplate.opsForZSet().reverseRange(RedisConstant.BANGUMI_INDEX_FAVORITE_CNT, 0, num - 1);
        if (set == null) return new ArrayList<>();

        var arr = new ArrayList<>(set.stream().map(Long::parseLong).toList());
        if (arr.size() < num) {
            var randomList = videoGroupMapper.selectList(new LambdaQueryWrapper<VideoGroup>()
                    .select(VideoGroup::getId)
                    .eq(VideoGroup::getVideoGroupStatus, VideoGroup.Status.ENABLE)
                    .eq(VideoGroup::getType, VideoGroup.Type.ANIME_VIDEO_GROUP)
                    .notIn(!arr.isEmpty(), VideoGroup::getId, arr)
                    .last("order by rand() limit " + (num - arr.size()))
            );
            randomList.forEach(videoGroup -> arr.add(videoGroup.getId()));
        }
        return arr;
    }

    /**
     * 会自动判断视频组类型
     */
    public void recordWatch(Long videoGroupId) {
        if (videoGroupServiceBase.getVideoGroupType(videoGroupId) == null) return;
        if (!Objects.equals(videoGroupServiceBase.getVideoGroupType(videoGroupId), VideoGroup.Type.ANIME_VIDEO_GROUP)) return;
        stringRedisTemplate.opsForZSet().incrementScore(RedisConstant.BANGUMI_INDEX_WATCH_CNT, videoGroupId + "", 1);
    }

    public List<Long> listWatch(int num) {
        var set = stringRedisTemplate.opsForZSet().reverseRange(RedisConstant.BANGUMI_INDEX_WATCH_CNT, 0, num - 1);
        if (set == null) return new ArrayList<>();

        var arr = new ArrayList<>(set.stream().map(Long::parseLong).toList());
        if (arr.size() < num) {
            var randomList = videoGroupMapper.selectList(new LambdaQueryWrapper<VideoGroup>()
                    .select(VideoGroup::getId)
                    .eq(VideoGroup::getVideoGroupStatus, VideoGroup.Status.ENABLE)
                    .eq(VideoGroup::getType, VideoGroup.Type.ANIME_VIDEO_GROUP)
                    .notIn(!arr.isEmpty(), VideoGroup::getId, arr)
                    .last("order by rand() limit " + (num - arr.size()))
            );
            randomList.forEach(videoGroup -> arr.add(videoGroup.getId()));
        }
        return arr;
    }


    /**
     * 最多返回50条
     */
    public List<VideoGroupWithDataVO> listBangumi(Byte type) {
        var self = SpringContextUtil.getBean(getClass());
        return self.listBangumiCache(type)
                .stream().map(videoGroupId -> videoGroupServiceBase.getVideoGroupWithData(videoGroupId))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 最多返回50条
     */
    @Cacheable(cacheNames = RedisConstant.BANGUMI_INDEX_IDS, key = "#type", unless = "#result == null")
    public List<Long> listBangumiCache(Byte type) {
        final int pageSize = 50;
        if (Objects.equals(type, BangumiIndexType.FAVORITE_CNT.TYPE)) {
            return listFavorite(pageSize);
        } else if (Objects.equals(type, BangumiIndexType.UPDATE_TIME.TYPE)) {
            return bangumiVideoGroupMapper.listIdsByUpdateTimeDesc(pageSize, VideoGroup.Status.ENABLE);
        } else if (Objects.equals(type, BangumiIndexType.WATCH_CNT.TYPE)) {
            return listWatch(pageSize);
        }
        return null;
    }
}
