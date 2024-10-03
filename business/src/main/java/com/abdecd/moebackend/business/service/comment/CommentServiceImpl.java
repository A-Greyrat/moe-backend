package com.abdecd.moebackend.business.service.comment;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.common.util.UnionFind;
import com.abdecd.moebackend.business.dao.entity.UserComment;
import com.abdecd.moebackend.business.dao.mapper.UserCommentMapper;
import com.abdecd.moebackend.business.pojo.dto.comment.AddCommentDTO;
import com.abdecd.moebackend.business.pojo.vo.comment.UserCommentVO;
import com.abdecd.moebackend.business.pojo.vo.comment.UserCommentVOBasic;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.result.PageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Nonnull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private UserCommentMapper userCommentMapper;
    @Autowired
    private RedisTemplate<String, LocalDateTime> redisTemplate;

    @Override
    public PageVO<List<UserCommentVO>> getComment(Long videoId, Integer page, Integer pageSize) {
        var commentService = SpringContextUtil.getBean(CommentServiceImpl.class);
        var userCommentList = commentService.getCachedCommentsBySthId(videoId);
        if (userCommentList.isEmpty()) return new PageVO<>(0, new ArrayList<>());
        var cnt = userCommentMapper.countRootCommentBySthId(videoId, UserComment.Status.ENABLE);
        if (page * pageSize > userCommentList.size()) {
            // 超出范围且后面有数据就无缓
            if (cnt > userCommentList.size()) userCommentList = commentService.getCommentsBySthId(videoId);
        }
        return new PageVO<>(cnt, userCommentList.subList(Math.max(0, (page - 1) * pageSize), Math.min(userCommentList.size(), page * pageSize)));
    }

    /**
     * 最多缓存前 xx 条
     */
    @Cacheable(value = RedisConstant.VIDEO_COMMENT, key = "#videoId", unless = "#result.isEmpty()")
    @Nonnull
    public List<List<UserCommentVO>> getCachedCommentsBySthId(Long videoId) {
        var list = getCommentsBySthId(videoId);
        if (list == null || list.isEmpty()) return new ArrayList<>();
        var maxCnt = RedisConstant.VIDEO_COMMENT_CACHE_SIZE;
        var lastIndex = 0;
        for (var userCommentList : list) {
            if ((maxCnt -= userCommentList.size()) >= 0) {
                lastIndex++;
            } else break;
        }
        return new ArrayList<>(list.subList(0, lastIndex));
    }

    public List<List<UserCommentVO>> getCommentsBySthId(Long videoId) {
        var allComments = userCommentMapper.listCommentBySthId(videoId, null);
        if (allComments.isEmpty()) return null;

        // todo 并查集 long 支持
        var unionFind = new UnionFind(Math.toIntExact(allComments.getLast().getId()) + 1);
        for (var userComment : allComments) {
            if (userComment.getToId() == -1) continue;
            unionFind.union(Math.toIntExact(userComment.getId()), Math.toIntExact(userComment.getToId()));
        }

        var result = new LinkedHashMap<Integer, List<UserCommentVOBasic>>();
        for (var userComment : allComments) {
            if (Objects.equals(userComment.getStatus(), UserComment.Status.DELETED)) continue;
            // 根评论已建立，直接附加
            if (result.containsKey(unionFind.find(Math.toIntExact(userComment.getId())))) {
                result.get(unionFind.find(Math.toIntExact(userComment.getId()))).add(userComment);
            } else {
                // 建立根评论块
                // 第一个一般是根评论（最早创建）
                // 不是根评论说明根评论删掉了，整块不显示
                if (userComment.getToId() != -1) continue;
                ArrayList<UserCommentVOBasic> list = new ArrayList<>();
                list.add(userComment);
                result.put(unionFind.find(Math.toIntExact(userComment.getId())), list);
            }
        }
        return new ArrayList<>(result.values()
                .stream().map(list -> new ArrayList<>(list.stream().map(item -> {
                            var userCommentVO = new UserCommentVO();
                            BeanUtils.copyProperties(item, userCommentVO);
                            return userCommentVO;
                        }).toList())
                ).toList());
    }

    @CacheEvict(value = RedisConstant.VIDEO_COMMENT, key = "#addCommentDTO.videoId")
    @Override
    public Long addComment(AddCommentDTO addCommentDTO) {
        var userComment = addCommentDTO.toEntity();
        // 检查评论的id是否在这个视频中
        if (addCommentDTO.getToId() != -1) {
            var beCommented = userCommentMapper.selectOne(new LambdaQueryWrapper<UserComment>()
                    .eq(UserComment::getId, userComment.getToId())
                    .eq(UserComment::getVideoId, userComment.getVideoId())
                    .eq(UserComment::getStatus, UserComment.Status.ENABLE)
            );
            if (beCommented == null) return (long) -1;
        }
        userCommentMapper.insert(userComment);
        // 更新评论时间戳
        redisTemplate.opsForValue().set(
                RedisConstant.TIMESTAMP_VIDEO_COMMENT + userComment.getVideoId(),
                userComment.getTimestamp(),
                10,
                TimeUnit.DAYS
        );
        return userComment.getId();
    }

    @CacheEvict(value = RedisConstant.VIDEO_COMMENT, key = "#root.target.getSthIdByCommentId(#id)")
    @Override
    public void deleteComment(Long id) {
        var effectedRows = userCommentMapper.update(new LambdaUpdateWrapper<UserComment>()
                .eq(UserComment::getId, id)
                .eq(UserComment::getUserId, UserContext.getUserId())
                .set(UserComment::getStatus, UserComment.Status.DELETED)
                .set(UserComment::getTimestamp, LocalDateTime.now())
        );
        if (effectedRows != 0) {
            // 更新评论时间戳
            redisTemplate.opsForValue().set(
                    RedisConstant.TIMESTAMP_VIDEO_COMMENT + getSthIdByCommentId(id),
                    LocalDateTime.now(),
                    10,
                    TimeUnit.DAYS
            );
        }
    }

    @CacheEvict(value = RedisConstant.VIDEO_COMMENT, key = "#root.target.getSthIdByCommentId(#id)")
    @Override
    public void forceDeleteComment(Long id) {
        var effectedRows = userCommentMapper.update(new LambdaUpdateWrapper<UserComment>()
                .eq(UserComment::getId, id)
                .set(UserComment::getStatus, UserComment.Status.DELETED)
                .set(UserComment::getTimestamp, LocalDateTime.now())
        );
        if (effectedRows != 0) {
            // 更新评论时间戳
            redisTemplate.opsForValue().set(
                    RedisConstant.TIMESTAMP_VIDEO_COMMENT + getSthIdByCommentId(id),
                    LocalDateTime.now(),
                    10,
                    TimeUnit.DAYS
            );
        }
    }

    @Cacheable(value = RedisConstant.VIDEO_COMMENT_CNT, key = "#videoId")
    @Override
    public Long getCommentCount(Long videoId) {
        return userCommentMapper.selectCount(new LambdaQueryWrapper<UserComment>()
                .eq(UserComment::getVideoId, videoId)
        );
    }

    @SuppressWarnings("unused")
    public Long getSthIdByCommentId(Long id) {
        var obj = userCommentMapper.selectById(id);
        if (obj == null) return (long) -1;
        return obj.getVideoId();
    }
}
