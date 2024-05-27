package com.abdecd.moebackend.business.pojo.vo.video;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class VideoForceVO extends VideoVO {
    @JsonGetter
    public Byte getStatus() {
        return status;
    }
}
