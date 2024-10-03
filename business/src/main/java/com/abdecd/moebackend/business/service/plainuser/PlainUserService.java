package com.abdecd.moebackend.business.service.plainuser;

import com.abdecd.moebackend.business.controller.base.CommonController;
import com.abdecd.moebackend.business.dao.entity.PlainUserDetail;
import com.abdecd.moebackend.business.dao.entity.User;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.PlainUserDetailMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.dao.service.UserService;
import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.pojo.dto.plainuser.UpdatePlainUserDTO;
import com.abdecd.moebackend.business.service.fileservice.FileService;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class PlainUserService {
    @Autowired
    private PlainUserDetailMapper plainUserDetailMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;
    @Autowired
    private CommonController commonController;
    @Autowired
    private VideoGroupMapper videoGroupMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Cacheable(value = RedisConstant.PLAIN_USER_DETAIL, key = "#uid", unless = "#result == null")
    public PlainUserDetail getPlainUserDetail(Long uid) {
        return plainUserDetailMapper.selectById(uid);
    }

    @CacheEvict(value = RedisConstant.PLAIN_USER_DETAIL, key = "#root.target.getCurrUserId()")
    @Transactional
    public void updatePlainUserDetail(UpdatePlainUserDTO updatePlainUserDTO) {
        String newFileUrl = null;
        String oldFileUrl = null;
        if (updatePlainUserDTO.getAvatar() != null) {
            try {
                var result = commonController.uploadImg(updatePlainUserDTO.getAvatar());
                if (Objects.requireNonNull(result.join().getBody()).getCode() != 200) throw new Exception();
                var avatarBody = result.join().getBody();
                if (avatarBody == null) throw new Exception();
                var avatar = avatarBody.getData();
                newFileUrl = fileService.changeTmpFileToStatic(avatar, "/user/" + UserContext.getUserId(), "avatar-" + UUID.randomUUID() + avatar.substring(avatar.lastIndexOf('.')));
                oldFileUrl = plainUserDetailMapper.selectById(UserContext.getUserId()).getAvatar();
            } catch (Exception e) {
                throw new BaseException(MessageConstant.IMG_FILE_UPLOAD_FAIL);
            }
        }
        if (updatePlainUserDTO.getNickname() != null) {
            // 昵称重复
            if (userService.exists(new LambdaQueryWrapper<User>()
                    .eq(User::getNickname, updatePlainUserDTO.getNickname())
            )) throw new BaseException(MessageConstant.USER_DULPLICATE);
            userService.update(new LambdaUpdateWrapper<User>()
                    .eq(User::getId, UserContext.getUserId())
                    .set(User::getNickname, updatePlainUserDTO.getNickname())
            );
            userService.getUserCache().delete(UserContext.getUserId() + "");
        }
        var entity = updatePlainUserDTO.toEntity(UserContext.getUserId());
        if (newFileUrl != null) entity.setAvatar(newFileUrl);
        plainUserDetailMapper.updateById(entity);
        if (oldFileUrl != null) fileService.deleteFile(oldFileUrl);
        // 清理缓存
        clearVideoGroupCache(UserContext.getUserId());
    }

    @CacheEvict(value = RedisConstant.PLAIN_USER_DETAIL, key = "#root.target.getCurrUserId()")
    @Transactional
    public void deleteCurrPlainUserDetail() {
        var user = User.toBeDeleted(UserContext.getUserId());
        plainUserDetailMapper.updateById(new PlainUserDetail()
            .setUserId(UserContext.getUserId())
            .setNickname(user.getNickname())
            .setAvatar("")
            .setSignature("")
        );
        // 清理缓存
        clearVideoGroupCache(UserContext.getUserId());
    }

    private void clearVideoGroupCache(Long userId) {
        var list = videoGroupMapper.selectList(new LambdaQueryWrapper<VideoGroup>()
                .eq(VideoGroup::getUserId, userId)
        );
        if (list.isEmpty()) return;
        for (var videoGroup : list) {
            stringRedisTemplate.delete(RedisConstant.VIDEO_GROUP_CACHE + "::" + videoGroup.getId());
            stringRedisTemplate.delete(RedisConstant.BANGUMI_VIDEO_GROUP_CACHE + "::" + videoGroup.getId());
        }
    }

    @SuppressWarnings("unused")
    public Long getCurrUserId() {
        return UserContext.getUserId();
    }
}
