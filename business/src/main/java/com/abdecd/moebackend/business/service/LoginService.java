package com.abdecd.moebackend.business.service;

import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.dao.dataencrypt.EncryptStrHandler;
import com.abdecd.moebackend.business.dao.entity.PlainUserDetail;
import com.abdecd.moebackend.business.dao.entity.User;
import com.abdecd.moebackend.business.dao.mapper.PlainUserDetailMapper;
import com.abdecd.moebackend.business.dao.service.UserService;
import com.abdecd.moebackend.business.pojo.dto.user.*;
import com.abdecd.moebackend.business.service.common.CommonService;
import com.abdecd.moebackend.business.service.plainuser.PlainUserService;
import com.abdecd.moebackend.business.tokenLogin.common.util.PwdUtil;
import com.abdecd.moebackend.business.tokenLogin.service.TokenLoginService;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
public class LoginService {
    @Autowired
    UserService userService;
    @Autowired
    TokenLoginService tokenLoginService;
    @Autowired
    CommonService commonService;
    @Autowired
    PlainUserDetailMapper plainUserDetailMapper;
    @Autowired
    PlainUserService plainUserService;

    public User login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        User willLoginUser = null;
        try {
            willLoginUser = userService.getUserById(Long.parseLong(username));
        } catch (NumberFormatException ignored) {
        }
        if (willLoginUser == null) {
            willLoginUser = userService.getUserByEmail(username);
        }

        // 进行登录
        var user = willLoginUser;
        // 用户不存在
        if (user == null) return null;
        // 用户被删除
        if (Objects.equals(user.getStatus(), User.Status.DELETED)) return null;
        if (Objects.equals(user.getStatus(), User.Status.LOCKED)) {
            //账号被锁定
            throw new BaseException(MessageConstant.ACCOUNT_LOCKED);
        }
        String rawPwd;
        try {
            rawPwd = tokenLoginService.convertPwd(password);
        } catch (Exception e) {
            throw new BaseException(e.getMessage());
        }
        if (!PwdUtil.verifyPwd(rawPwd, user.getPassword())) {
            //密码错误
            throw new BaseException(MessageConstant.LOGIN_PASSWORD_ERROR);
        }
        return user;
    }

    public User loginByEmail(LoginByEmailDTO loginByEmailDTO) {
        // 登录
        var user = userService.getUserByEmail(loginByEmailDTO.getEmail());

        // 用户不存在
        if (user == null) return null;
        // 用户被删除
        if (Objects.equals(user.getStatus(), User.Status.DELETED)) return null;
        if (Objects.equals(user.getStatus(), User.Status.LOCKED)) {
            //账号被锁定
            throw new BaseException(MessageConstant.ACCOUNT_LOCKED);
        }
        return user;
    }

    @Transactional
    public User signup(SignUpDTO signUpDTO) {
        // 检验重复
        if (userService.exists(new LambdaQueryWrapper<User>()
            .eq(User::getEmail, EncryptStrHandler.encrypt(signUpDTO.getEmail()))
            .or().eq(User::getNickname, signUpDTO.getNickname())
        )) throw new BaseException(MessageConstant.USER_DULPLICATE);
        // 注册
        try {
            var user = User.ofEmpty()
                .setPermission("1")
                .setPassword(PwdUtil.encodePwd(tokenLoginService.convertPwd(signUpDTO.getPassword())))
                .setNickname(signUpDTO.getNickname())
                .setEmail(signUpDTO.getEmail());
            userService.save(user);

            plainUserDetailMapper.insert(new PlainUserDetail()
                .setUserId(user.getId())
                .setNickname(signUpDTO.getNickname())
                .setAvatar("")
                .setSignature("")
            );
            return user;
        } catch (Exception e) {
            throw new BaseException(e.getMessage());
        }
    }

    public void forgetPassword(ResetPwdDTO resetPwdDTO) {
        // 重置密码
        var user = userService.getOne(new LambdaQueryWrapper<User>()
            .eq(User::getEmail, EncryptStrHandler.encrypt(resetPwdDTO.getEmail()))
        );
        if (user == null) throw new BaseException(MessageConstant.USER_NOT_EXIST);
        try {
            userService.updateById(new User()
                .setId(user.getId())
                .setPassword(PwdUtil.encodePwd(tokenLoginService.convertPwd(resetPwdDTO.getNewPassword())))
            );
            // 删除缓存
            userService.getUserCache().delete(user.getId() + "");
            tokenLoginService.forceLogout(user.getId());
        } catch (Exception e) {
            throw new BaseException(e.getMessage());
        }
    }

    public void changeEmail(Long userId, ChangeEmailDTO changeEmailDTO) {
        var user = userService.getById(userId);
        if (user == null) throw new BaseException(MessageConstant.USER_NOT_EXIST);
        var oldEmail = user.getEmail();
        // 检验重复
        if (userService.exists(new LambdaQueryWrapper<User>()
            .eq(User::getEmail, EncryptStrHandler.encrypt(changeEmailDTO.getNewEmail()))
        )) throw new BaseException(MessageConstant.USER_DULPLICATE);
        userService.updateById(user.setEmail(changeEmailDTO.getNewEmail()));
        // 删除缓存
        userService.getUserCache().delete(user.getId() + "");
        userService.getUserEmailKeyCache().delete(oldEmail);
    }

    public void deleteAccount(Long userId, String verifyCode) {
        var user = userService.getById(userId);
        if (user == null) throw new BaseException(MessageConstant.USER_NOT_EXIST);
        if (Objects.equals(user.getStatus(), User.Status.LOCKED))
            throw new BaseException(MessageConstant.ACCOUNT_LOCKED);
        // 验证邮箱
        commonService.verifyEmail(user.getEmail(), verifyCode);
        // 删除账号
        userService.updateById(User.toBeDeleted(user.getId()));
        plainUserService.deleteCurrPlainUserDetail();
        // 删除缓存
        userService.getUserCache().delete(user.getId() + "");
        userService.getUserEmailKeyCache().delete(user.getEmail());
        tokenLoginService.forceLogout(user.getId());
    }
}
