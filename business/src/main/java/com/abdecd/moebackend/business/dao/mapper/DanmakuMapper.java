package com.abdecd.moebackend.business.dao.mapper;

import com.abdecd.moebackend.business.dao.entity.Danmaku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DanmakuMapper extends BaseMapper<Danmaku> {
    int PARTITION_SIZE = 10;

    @Select("""
        SELECT count(1) FROM INFORMATION_SCHEMA.PARTITIONS
        WHERE TABLE_NAME = 'danmaku'
        AND PARTITION_NAME = #{partitionName};
        """)
    boolean hasPartition(String partitionName);

    @Update("""
        ALTER TABLE danmaku
        ADD PARTITION (
        PARTITION ${partitionName} VALUES LESS THAN (#{maxVideoId})
        );
        """)
    void createPartition(String partitionName, Long maxVideoId);

    default void refreshPartition(Long videoId) {
        if (videoId == null) return;
        if (hasPartition("p" + videoId / PARTITION_SIZE)) return;
        createPartition(
            "p" + videoId / PARTITION_SIZE,
            (videoId / PARTITION_SIZE + 1) * PARTITION_SIZE
        );
    }
}
