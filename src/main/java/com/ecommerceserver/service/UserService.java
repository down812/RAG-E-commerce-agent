package com.ecommerceserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerceserver.model.dto.UserAuthDTO;
import com.ecommerceserver.model.dto.UserLoginDTO;
import com.ecommerceserver.model.entity.User;
import com.ecommerceserver.model.vo.UserAuthVO;
import com.ecommerceserver.model.vo.UserLoginVO;

/**
 * 用户信息授权服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户统一登录
     * @param userLoginDTO
     * @return
     */
    UserLoginVO login(UserLoginDTO userLoginDTO);



    /**
     * 用户信息授权
     * 将未授权用户（type=2）转换为已登录用户，完善用户信息
     * @param userAuthDTO 授权信息
     * @return 授权后的用户信息
     */
    UserAuthVO authUser(UserAuthDTO userAuthDTO);



    /**
     * 退出登录
     * @param token 用户登录时的令牌
     */
    void logout(String token);
}
