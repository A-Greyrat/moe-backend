package com.abdecd.moebackend.business.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Accessors(chain = true)
@Data
@TableName("bangumi_time_table")
public class BangumiTimeTable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long videoGroupId;
    private Long videoId;
    /**
     * 预定的发布时间
     */
    private LocalDateTime updateTime;
    /**
     * 0已处理 1未处理
     */
    private Byte status;
}
