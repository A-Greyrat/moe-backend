package com.abdecd.moebackend.business.service.PlainUserManage;

import com.abdecd.moebackend.business.dao.entity.User;
import com.abdecd.moebackend.business.dao.entity.UserManage;
import com.abdecd.moebackend.business.dao.mapper.PlainUserManageMapper;
import com.abdecd.moebackend.business.dao.service.UserService;
import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.pojo.dto.plainuser.BanUserDTO;
import com.abdecd.moebackend.business.pojo.vo.plainuser.AllVO;
import com.abdecd.moebackend.business.tokenLogin.service.TokenLoginService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlainUserManageServierImpl implements PlainUserManageService {

    @Autowired
    private PlainUserManageMapper plainUserManageMapper;
    @Autowired
    private TokenLoginService tokenLoginService;
    @Autowired
    private UserService userService;

    @Override
    public Page<AllVO> listUsers(Long id, String name, Integer status, int page, int pageSize) {
        Page<UserManage> userPage = new Page<>(page, pageSize);
        return plainUserManageMapper.selectUsers(userPage, id, name, status);
    }

    @Override
    public boolean banUser(BanUserDTO banUserDTO) {
        UserManage user = plainUserManageMapper.selectById(banUserDTO.getId());
        if (user == null) {
            return false;
        }
        if (banUserDTO.getStatus() == User.Status.LOCKED) {
            tokenLoginService.forceLogout(user.getId());
        }
        user.setStatus(banUserDTO.getStatus());
        userService.getUserCache().delete(user.getId() + "");
        return plainUserManageMapper.updateById(user) == 1;
    }

    @Override
    public void updatePermission(Long id, String permission) {
        var user = plainUserManageMapper.selectById(id);
        if (user == null) throw new BaseException("用户不存在");
        user.setPermission(permission);
        plainUserManageMapper.updateById(user);
        userService.getUserCache().delete(user.getId() + "");
        tokenLoginService.forceLogout(user.getId());
    }
}
