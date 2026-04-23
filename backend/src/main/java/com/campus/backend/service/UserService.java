package com.campus.backend.service;

import com.campus.backend.dto.UserRegisterDTO;
import com.campus.backend.dto.UserVO;
import com.campus.backend.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    UserVO register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     */
    UserVO login(String username, String password);

    /**
     * 根据ID获取用户信息
     */
    UserVO getUserInfo(Long userId);

    /**
     * 根据用户名获取用户信息
     */
    UserVO getUserInfoByUsername(String username);

    /**
     * 更新用户资料 (昵称、头像、学校等)
     */
    UserVO updateProfile(Long userId, User profileData);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 获取用户实体（内部使用，如其他Service调用）
     */
    User getUserEntityById(Long userId);
}
