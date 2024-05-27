package com.abdecd.moebackend.business.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Accessors(chain = true)
@Data
@TableName("bangumi_video_group")
public class BangumiVideoGroup {
    @TableId(type = IdType.NONE)
    private Long videoGroupId;
    private LocalDateTime releaseTime;
    private String updateAtAnnouncement;
    private Integer status;
    private LocalDateTime updateTime;

    public static class Status {
        static final Integer FINISHED = 0;
        static final Integer LOADING = 1;
    }
}
