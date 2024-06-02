package com.abdecd.moebackend.business.service.PlainUserManage;

import com.abdecd.moebackend.business.pojo.dto.plainuser.BanUserDTO;
import com.abdecd.moebackend.business.pojo.vo.plainuser.AllVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface PlainUserManageService {
    Page<AllVO> listUsers(Long id, String name, Integer status, int page, int pageSize);
    boolean banUser(BanUserDTO banUserDTO);

    void updatePermission(Long id, String permissions);
}
