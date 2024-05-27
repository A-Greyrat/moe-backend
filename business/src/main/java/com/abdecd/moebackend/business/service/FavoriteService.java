package com.abdecd.moebackend.business.service;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.dao.entity.PlainUserFavorite;
import com.abdecd.moebackend.business.dao.entity.PlainUserLike;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.PlainUserFavoriteMapper;
import com.abdecd.moebackend.business.dao.mapper.PlainUserLikeMapper;
import com.abdecd.moebackend.business.pojo.vo.favorite.BangumiVideoGroupFavoriteVO;
import com.abdecd.moebackend.business.pojo.vo.favorite.FavoriteVO;
import com.abdecd.moebackend.business.service.plainuser.PlainUserHistoryService;
import com.abdecd.moebackend.business.service.videogroup.BangumiVideoGroupServiceBase;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.constant.StatusConstant;
import com.abdecd.moebackend.common.result.PageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;

@Service
public class FavoriteService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;
    @Autowired
    private PlainUserHistoryService plainUserHistoryService;
    @Autowired
    private BangumiVideoGroupServiceBase bangumiVideoGroupServiceBase;
    @Autowired
    private PlainUserFavoriteMapper plainUserFavoriteMapper;
    @Autowired
    private PlainUserLikeMapper plainUserLikeMapper;

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.FAVORITE_PLAIN, key = "#userId"),
            @CacheEvict(cacheNames = RedisConstant.FAVORITE_BANGUMI, key = "#userId"),
            @CacheEvict(cacheNames = RedisConstant.IS_USER_FAVORITE, key = "#userId + ':' + #videoGroupId")
    })
    public void add(Long userId, Long videoGroupId) {
        // 如果不存在
        if (videoGroupServiceBase.getVideoGroupType(videoGroupId) == null)
            throw new BaseException(MessageConstant.INVALID_VIDEO_GROUP);
        // 如果超过最大收藏数
        if (
                plainUserFavoriteMapper.selectCount(
                        new LambdaQueryWrapper<PlainUserFavorite>()
                        .eq(PlainUserFavorite::getUserId, userId)
                ) >= RedisConstant.FAVORITES_SIZE
        ) throw new BaseException(MessageConstant.FAVORITES_EXCEED_LIMIT);
        // 如果已经加过了
        if (
                plainUserFavoriteMapper.selectOne(
                        new LambdaQueryWrapper<PlainUserFavorite>()
                                .eq(PlainUserFavorite::getUserId, userId)
                                .eq(PlainUserFavorite::getVideoGroupId, videoGroupId)
                ) != null
        ) throw new BaseException(MessageConstant.FAVORITES_EXIST);
        plainUserFavoriteMapper.insert(new PlainUserFavorite()
                .setUserId(userId)
                .setVideoGroupId(videoGroupId)
        );
        // 添加到视频组收藏量
        stringRedisTemplate.opsForValue().increment(RedisConstant.VIDEO_GROUP_FAVORITE_CNT + videoGroupId);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.FAVORITE_PLAIN, key = "#userId"),
            @CacheEvict(cacheNames = RedisConstant.FAVORITE_BANGUMI, key = "#userId"),
    })
    public void delete(Long userId, long[] videoGroupIds) {
        Long[] ids = new Long[videoGroupIds.length];
        for (int i = 0; i < videoGroupIds.length; i++) {
            ids[i] = videoGroupIds[i];
        }
        var cnt = plainUserFavoriteMapper.selectCount(new LambdaQueryWrapper<PlainUserFavorite>()
                .eq(PlainUserFavorite::getUserId, userId)
                .in(PlainUserFavorite::getVideoGroupId, (Object[]) ids)
        );
        if (cnt == null || cnt != videoGroupIds.length) throw new BaseException(MessageConstant.INVALID_VIDEO_GROUP);

        for (var videoGroupId : videoGroupIds) {
            if (
                    plainUserFavoriteMapper.delete(
                            new LambdaUpdateWrapper<PlainUserFavorite>()
                                    .eq(PlainUserFavorite::getUserId, userId)
                                    .eq(PlainUserFavorite::getVideoGroupId, videoGroupId)
                    ) == 1
            ) {
                stringRedisTemplate.opsForValue().decrement(RedisConstant.VIDEO_GROUP_FAVORITE_CNT + videoGroupId);
                // 清除缓存
                stringRedisTemplate.delete(RedisConstant.IS_USER_FAVORITE.substring(0, RedisConstant.IS_USER_FAVORITE.lastIndexOf("#")) + "::" + userId + ":" + videoGroupId);
            }
        }
    }

//    @Cacheable(value = RedisConstant.FAVORITE_PLAIN, key = "#userId", unless = "#page != 1 || #pageSize != 10 || #result.total == 0")
    public PageVO<FavoriteVO> getPlainFavorite(Long userId, Integer page, Integer pageSize) {
        var total = plainUserFavoriteMapper.countFavoriteWithType(userId, VideoGroup.Type.PLAIN_VIDEO_GROUP);
        var result = plainUserFavoriteMapper.pageFavoriteWithType(userId, VideoGroup.Type.PLAIN_VIDEO_GROUP, Math.max(0, (page - 1) * pageSize), pageSize);
        var arr = result
                .stream()
                .map(favorite -> new FavoriteVO().setVideoGroupVO(videoGroupServiceBase.getVideoGroupInfo(favorite.getVideoGroupId()))).toList();
        return new PageVO<>(Math.toIntExact(total), new ArrayList<>(arr));
    }

//    @Cacheable(value = RedisConstant.FAVORITE_BANGUMI, key = "#userId", unless = "#page != 1 || #pageSize != 10 || #result.total == 0")
    public PageVO<FavoriteVO> getBangumiFavorite(Long userId, Integer page, Integer pageSize) {
        var total = plainUserFavoriteMapper.countFavoriteWithType(userId, VideoGroup.Type.ANIME_VIDEO_GROUP);
        var result = plainUserFavoriteMapper.pageFavoriteWithType(userId, VideoGroup.Type.ANIME_VIDEO_GROUP, Math.max(0, (page - 1) * pageSize), pageSize);
        var arr = result
                .stream()
                .map(favorite -> formBangumiFavorite(favorite.getVideoGroupId())).toList();
        return new PageVO<>(Math.toIntExact(total), new ArrayList<>(arr));
    }

    public FavoriteVO formBangumiFavorite(Long videoGroupId) {
        var info = videoGroupServiceBase.getVideoGroupInfo(videoGroupId);
        var contents = bangumiVideoGroupServiceBase.getContents(videoGroupId);
        var latestVideoTitle = contents.isEmpty() ? null : contents.getFirst().getTitle();
        var lastWatch = plainUserHistoryService.getLatestHistory(videoGroupId);
        var lastWatchVideoTitle = lastWatch == null ? null : lastWatch.getVideoTitle();
        var lastWatchVideoId = lastWatch == null ? null : lastWatch.getVideoId();
        var lastWatchVideoIndex = lastWatch == null ? null : lastWatch.getVideoIndex();
        var vo = new BangumiVideoGroupFavoriteVO();
        vo.setVideoGroupVO(info);
        vo.setLatestVideoTitle(latestVideoTitle);
        vo.setLastWatchVideoTitle(lastWatchVideoTitle);
        vo.setLastWatchVideoId(lastWatchVideoId);
        vo.setLastWatchVideoIndex(lastWatchVideoIndex);
        return vo;
    }

    public Long getVideoGroupFavoriteCount(Long videoGroupId) {
        var count = stringRedisTemplate.opsForValue().get(RedisConstant.VIDEO_GROUP_FAVORITE_CNT + videoGroupId);
        if (count == null) return 0L;
        return Long.parseLong(count);
    }

    @Cacheable(value = RedisConstant.IS_USER_FAVORITE, key = "#userId + ':' + #videoGroupId")
    public boolean isUserFavorite(Long userId, Long videoGroupId) {
        return plainUserFavoriteMapper.selectOne(new LambdaQueryWrapper<PlainUserFavorite>()
                .eq(PlainUserFavorite::getUserId, userId)
                .eq(PlainUserFavorite::getVideoGroupId, videoGroupId)
        ) != null;
    }

    @CacheEvict(value = RedisConstant.IS_USER_LIKE, key = "#userId + ':' + #videoGroupId")
    public void addOrDeleteLike(Long userId, Long videoGroupId, Byte status) {
        if (videoGroupServiceBase.getVideoGroupInfo(videoGroupId) == null) throw new BaseException(MessageConstant.INVALID_VIDEO_GROUP);
        var previous = plainUserLikeMapper.selectOne(new LambdaQueryWrapper<PlainUserLike>()
                .eq(PlainUserLike::getUserId, userId)
                .eq(PlainUserLike::getVideoGroupId, videoGroupId)
        );
        if (Objects.equals(status, StatusConstant.DISABLE)) {
            if (previous == null) throw new BaseException(MessageConstant.LIKE_NOT_EXIST);
            plainUserLikeMapper.deleteById(previous);
            stringRedisTemplate.opsForValue().decrement(RedisConstant.VIDEO_GROUP_LIKE_CNT + videoGroupId);
        } else if (Objects.equals(status, StatusConstant.ENABLE)) {
            if (previous != null) throw new BaseException(MessageConstant.LIKE_EXIST);
            plainUserLikeMapper.insert(new PlainUserLike().setUserId(userId).setVideoGroupId(videoGroupId));
            stringRedisTemplate.opsForValue().increment(RedisConstant.VIDEO_GROUP_LIKE_CNT + videoGroupId);
        }
    }

    public Long getVideoGroupLikeCount(Long videoGroupId) {
        var count = stringRedisTemplate.opsForValue().get(RedisConstant.VIDEO_GROUP_LIKE_CNT + videoGroupId);
        if (count == null) return 0L;
        return Long.parseLong(count);
    }

    @Cacheable(value = RedisConstant.IS_USER_LIKE, key = "#userId + ':' + #videoGroupId")
    public boolean isUserLike(Long userId, Long videoGroupId) {
        return plainUserLikeMapper.selectOne(new LambdaQueryWrapper<PlainUserLike>()
                .eq(PlainUserLike::getUserId, userId)
                .eq(PlainUserLike::getVideoGroupId, videoGroupId)
        ) != null;
    }
}
