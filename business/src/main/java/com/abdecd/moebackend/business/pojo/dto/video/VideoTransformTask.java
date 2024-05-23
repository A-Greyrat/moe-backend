package com.abdecd.moebackend.business.pojo.dto.video;

import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class VideoTransformTask {
    private String id;
    private Long videoId;
    private String originPath;
    private String[] targetPaths;
    private Status[] status;

    /**
     * 回调函数名称 例如 videoServiceImpl.addVideo
     * @回调函数需要有唯一参数VideoTransformTask
     */
    private String cbBeanNameAndMethodName;

    public enum Status {
        WAITING,
        SUCCESS
    }

    public enum TaskType {
        VIDEO_TRANSFORM_360P(0, "360p"),
        VIDEO_TRANSFORM_720P(1, "720p"),
        VIDEO_TRANSFORM_1080P(2, "1080p");
        public final int NUM;
        public final String NAME;
        TaskType(int num, String name) {
            this.NUM = num;
            this.NAME = name;
        }
    }
    public TaskType[] getTaskTypes() {
        return new TaskType[]{TaskType.VIDEO_TRANSFORM_360P, TaskType.VIDEO_TRANSFORM_720P, TaskType.VIDEO_TRANSFORM_1080P};
    }
}
