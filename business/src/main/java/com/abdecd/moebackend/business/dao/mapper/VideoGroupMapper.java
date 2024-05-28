package com.abdecd.moebackend.business.dao.mapper;

import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;

@Mapper
public interface VideoGroupMapper extends BaseMapper<VideoGroup> {
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="id", before=false, resultType=long.class)
    @Insert("INSERT INTO video_group (user_id, title, description, cover, create_time, type, weight,tags,status) VALUES(#{userId}, #{title}, #{description}, #{cover}, #{createTime}, #{type}, #{weight},#{tags},#{videoGroupStatus})")
    int insertVideoGroup(VideoGroup videoGroup); // todo bug fix

    void update_(VideoGroup videoGroup);

    @Select("SELECT * FROM video_group  LIMIT #{pageSize} OFFSET #{offset}")
    ArrayList<VideoGroup> selectbyPage(Integer offset, Integer pageSize);

    @Update("UPDATE video_group  SET tags = #{tags} WHERE id = #{id} AND tags != #{tags}")
    Integer updateTagsByID(VideoGroup videoGroup);

    @Update("UPDATE video_group SET status = #{status} WHERE id = #{id}")
    Integer updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
