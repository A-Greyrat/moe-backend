package com.abdecd.moebackend.business.dao.entity;

import com.abdecd.moebackend.business.dao.dataencrypt.EncryptStrHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName(value = "user", autoResultMap = true)
public class User {
    @TableId(type = IdType.AUTO)
    private Long id; // 必须
    private String password; // 必须
    private String permission; // 必须
    private Byte status; // 必须

    private String nickname;
    @TableField(typeHandler = EncryptStrHandler.class)
    private String email;
    private LocalDateTime createTime;

    public static class Status {
        public static final byte NORMAL = 0;
        public static final byte LOCKED = 1;
        public static final byte DELETED = 2;
    }

    public static User ofEmpty() {
        return new User()
            .setId(null)
            .setPassword("")
            .setPermission("")
            .setStatus(Status.NORMAL)
            .setNickname("")
            .setEmail("")
            .setCreateTime(LocalDateTime.now());
    }

    public static User toBeDeleted(Long userId) {
        return new User()
            .setId(userId)
            .setStatus(Status.DELETED)
            .setNickname("账号已删除")
            .setEmail(UUID.randomUUID() + "");
    }
}
