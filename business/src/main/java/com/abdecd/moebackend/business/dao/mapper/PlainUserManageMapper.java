package com.abdecd.moebackend.business.dao.mapper;

import com.abdecd.moebackend.business.dao.entity.UserManage;
import com.abdecd.moebackend.business.pojo.vo.plainuser.AllVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlainUserManageMapper extends BaseMapper<UserManage> {

    Page<AllVO> selectUsers(Page<?> page, @Param("id") Long id, @Param("name") String name, @Param("status") Integer status);
}
