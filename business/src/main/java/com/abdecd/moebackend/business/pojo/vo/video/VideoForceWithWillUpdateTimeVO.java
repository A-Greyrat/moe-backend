package com.abdecd.moebackend.business.pojo.vo.video;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Data
public class VideoForceWithWillUpdateTimeVO extends VideoForceVO {
    private LocalDateTime willUpdateTime;
}
