package com.abdecd.moebackend.business.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Accessors(chain = true)
@Data
@TableName("bangumi_video_group")
public class BangumiVideoGroup {
    private Long videoGroupId;
    private LocalDateTime releaseTime;
    private String updateAtAnnouncement;
    private Integer status;
}