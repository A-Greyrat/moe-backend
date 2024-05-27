package com.abdecd.moebackend.business.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer type;
    private Long targetId;
    private Long userId;
    private String reason;
    private LocalDateTime createTime;
    private Integer status;

    public static class Type {
        public static final int VIDEO = 0;
        public static final int COMMENT = 1;
    }
}
