package com.abdecd.moebackend.business.service.statistic;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.pojo.dto.statistic.VideoPlayDTO;
import com.abdecd.moebackend.business.pojo.vo.statistic.StatisticDataVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.ContentsItemVO;
import com.abdecd.moebackend.business.service.FavoriteService;
import com.abdecd.moebackend.business.service.comment.CommentService;
import com.abdecd.moebackend.business.service.danmaku.DanmakuService;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.constant.RedisConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticService {
    @Autowired
    private LastWatchTimeStatistic lastWatchTimeStatistic;
    @Autowired
    private TotalWatchTimeStatistic totalWatchTimeStatistic;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private CommentService commentService;
    @Autowired
    private DanmakuService danmakuService;

    public void cntVideoPlay(VideoPlayDTO videoPlayDTO, int addTime) {
        // 记录上次观看位置
        lastWatchTimeStatistic.add(videoPlayDTO.getVideoId(), videoPlayDTO.getWatchProgress());
        // 记录播放总时长
        totalWatchTimeStatistic.add(videoPlayDTO.getVideoId(), addTime);
    }

    public void cntWatchCnt(Long videoGroupId) {
        stringRedisTemplate.opsForValue().increment(
                RedisConstant.VIDEO_GROUP_WATCH_CNT + videoGroupId
        );
    }

    public StatisticDataVO getStatisticData(Long videoGroupId) {
        var favoriteService = SpringContextUtil.getBean(FavoriteService.class);
        // 获取播放量
        String watchCntStr = stringRedisTemplate.opsForValue().get(
                RedisConstant.VIDEO_GROUP_WATCH_CNT + videoGroupId
        );
        Long watchCnt = watchCntStr == null ? 0L : Long.parseLong(watchCntStr);
//        Long watchCnt = (long) (Math.random()*1000000);
        // 获取点赞量
        Long likeCnt = favoriteService.getVideoGroupLikeCount(videoGroupId);
//        Long likeCnt = (long) (Math.random()*100000);
        boolean userLike = favoriteService.isUserLike(UserContext.getUserId(), videoGroupId);
        // 获取收藏量
        Long favoriteCnt = favoriteService.getVideoGroupFavoriteCount(videoGroupId);
//        Long favoriteCnt = (long) (Math.random()*100000);
        boolean userFavorite = favoriteService.isUserFavorite(UserContext.getUserId(), videoGroupId);
        // 获取评论量
//        var videoGroupServiceBase = SpringContextUtil.getBean(VideoGroupServiceBase.class);
//        List<ContentsItemVO> videoContents = (List<ContentsItemVO>) videoGroupServiceBase.getContents(videoGroupId);
//        if (videoContents == null) videoContents = new ArrayList<>();
//        Long commentCnt = videoContents.stream().map(x->commentService.getCommentCount(x.getVideoId())).reduce(0L, Long::sum);
//        Long danmakuCnt = videoContents.stream().map(x->danmakuService.getDanmakuCount(x.getVideoId())).reduce(0L, Long::sum);
        return new StatisticDataVO()
                .setWatchCnt(watchCnt)
                .setLikeCnt(likeCnt)
                .setFavoriteCnt(favoriteCnt)
                .setUserLike(userLike)
                .setUserFavorite(userFavorite);
//                .setCommentCnt(commentCnt)
//                .setDanmakuCnt(danmakuCnt);
    }

    public StatisticDataVO getFullStatisticData(Long videoGroupId) {
        var favoriteService = SpringContextUtil.getBean(FavoriteService.class);
        // 获取播放量
        String watchCntStr = stringRedisTemplate.opsForValue().get(
                RedisConstant.VIDEO_GROUP_WATCH_CNT + videoGroupId
        );
        Long watchCnt = watchCntStr == null ? 0L : Long.parseLong(watchCntStr);
//        Long watchCnt = (long) (Math.random()*1000000);
        // 获取点赞量
        Long likeCnt = favoriteService.getVideoGroupLikeCount(videoGroupId);
//        Long likeCnt = (long) (Math.random()*100000);
        boolean userLike = favoriteService.isUserLike(UserContext.getUserId(), videoGroupId);
        // 获取收藏量
        Long favoriteCnt = favoriteService.getVideoGroupFavoriteCount(videoGroupId);
//        Long favoriteCnt = (long) (Math.random()*100000);
        boolean userFavorite = favoriteService.isUserFavorite(UserContext.getUserId(), videoGroupId);
        // 获取评论量
        var videoGroupServiceBase = SpringContextUtil.getBean(VideoGroupServiceBase.class);
        List<ContentsItemVO> videoContents = (List<ContentsItemVO>) videoGroupServiceBase.getContents(videoGroupId);
        if (videoContents == null) videoContents = new ArrayList<>();
        Long commentCnt = videoContents.stream().parallel().map(x->commentService.getCommentCount(x.getVideoId())).reduce(0L, Long::sum);
        Long danmakuCnt = videoContents.stream().parallel().map(x->danmakuService.getDanmakuCount(x.getVideoId())).reduce(0L, Long::sum);
        return new StatisticDataVO()
                .setWatchCnt(watchCnt)
                .setLikeCnt(likeCnt)
                .setFavoriteCnt(favoriteCnt)
                .setUserLike(userLike)
                .setUserFavorite(userFavorite)
                .setCommentCnt(commentCnt)
                .setDanmakuCnt(danmakuCnt);
    }
}
