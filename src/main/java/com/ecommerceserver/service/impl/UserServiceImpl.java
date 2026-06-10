package com.ecommerceserver.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerceserver.config.JwtConfig;
import com.ecommerceserver.exception.AuthFailException;
import com.ecommerceserver.exception.LoginFailException;
import com.ecommerceserver.mapper.UserMapper;
import com.ecommerceserver.model.dto.*;
import com.ecommerceserver.model.entity.User;
import com.ecommerceserver.model.vo.UserAuthVO;
import com.ecommerceserver.model.vo.UserLoginVO;
import com.ecommerceserver.service.UserService;
import com.ecommerceserver.utils.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ecommerceserver.constants.CommonConstant.*;
import static com.ecommerceserver.constants.MessageConstant.*;
import static com.ecommerceserver.constants.RedisConstant.*;



@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private JwtConfig jwtConfig;



    /**
     * 统一登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {

        // 1. 查询用户（原有逻辑）
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("identifier", userLoginDTO.getIdentifier())
        );
        if (user == null) {
            throw new LoginFailException(USER_NOT_EXIST);
        }

        // 2. 密码校验（原有逻辑）
        String encryptedPwd = DigestUtils.md5DigestAsHex((SALT + userLoginDTO.getPassword()).getBytes());
        if (!encryptedPwd.equals(user.getPassword())) {
            throw new LoginFailException(PASSWORD_ERROR);
        }

        // 3. 生成JWT令牌（使用统一方法，与游客令牌格式一致）
        Map<String, Object> claims = new HashMap<>();
        claims.put(LOGIN_USER_ID, user.getId());
        claims.put(LOGIN_USER_TYPE, user.getType());
        Map<String, Object> header = new HashMap<>();
        header.put("type", "jwt");
        String token = jwtUtil.createTokenWithHeader(claims, header); // 带自定义头部

        // 4. 存储到Redis（与游客逻辑一致）
        RedisUserMsgDTO redisUserMsgDTO = new RedisUserMsgDTO();
        BeanUtils.copyProperties(user, redisUserMsgDTO);
        redisUserMsgDTO.setType(user.getType());

        String msgKey = LOGIN_USER_TOKEN + token;
        redisTemplate.opsForValue().set(
                msgKey,
                JSONUtil.toJsonStr(redisUserMsgDTO),
                jwtConfig.getUserTtlMillis() / 1000,
                TimeUnit.SECONDS
        );

        String tokenKey = LOGIN_USER_TOKEN + user.getId();
        redisTemplate.opsForValue().set(
                tokenKey,
                token,
                jwtConfig.getUserTtlMillis() / 1000,
                TimeUnit.SECONDS
        );

        // 5. 返回结果
        return UserLoginVO.builder()
                .token(token)
                .userId(user.getId())
                .type(user.getType())
                .identifier(user.getIdentifier())
                .build();
    }


    /**
     * User info auth: registered users modify their info
     * @param userAuthDTO auth info
     * @return updated user info
     */
    @Override
    @Transactional
    public UserAuthVO authUser(UserAuthDTO userAuthDTO) {
        if (ObjectUtil.isEmpty(userAuthDTO) ||
                StringUtils.isBlank(userAuthDTO.getOldIdentifier()) ||
                StringUtils.isBlank(userAuthDTO.getNewIdentifier()) ||
                StringUtils.isBlank(userAuthDTO.getEmail()) ||
                StringUtils.isBlank(userAuthDTO.getPassword()) ||
                StringUtils.isBlank(userAuthDTO.getCode())) {
            throw new AuthFailException(PARAM_NOT_COMPLETE);
        }

        String email = userAuthDTO.getEmail();
        String inputCode = userAuthDTO.getCode();
        String redisKey = EMAIL_CODE_KEY + email;
        String cachedCode = redisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(cachedCode) || !cachedCode.equals(inputCode)) {
            throw new AuthFailException("Code error or expired");
        }

        String oldIdentifier = userAuthDTO.getOldIdentifier();
        String newIdentifier = userAuthDTO.getNewIdentifier();
        Date currentTime = new Date();

        User dbUser = this.lambdaQuery()
                .eq(User::getIdentifier, oldIdentifier)
                .one();

        if (ObjectUtil.isEmpty(dbUser)) {
            throw new AuthFailException(USER_NOT_EXIST);
        }

        if (dbUser.getType() != 0 && dbUser.getType() != 1 && dbUser.getType() != 2) {
            throw new AuthFailException("无效的用户类型");
        }

        User existUser = this.lambdaQuery()
                .eq(User::getIdentifier, newIdentifier)
                .ne(User::getId, dbUser.getId())
                .one();
        if (ObjectUtil.isNotEmpty(existUser)) {
            throw new AuthFailException(NEW_IDENTIFIER_ALREADY_TAKEN);
        }

        User emailUser = this.lambdaQuery()
                .eq(User::getEmail, email)
                .ne(User::getId, dbUser.getId())
                .one();
        if (ObjectUtil.isNotEmpty(emailUser)) {
            throw new AuthFailException(EMAIL_ALREADY_LINKED);
        }

        String encryptedPwd = DigestUtils.md5DigestAsHex((SALT + userAuthDTO.getPassword()).getBytes());
        dbUser.setIdentifier(newIdentifier);
        dbUser.setEmail(email);
        dbUser.setPassword(encryptedPwd);
        dbUser.setUpdatedAt(currentTime);
        dbUser.setLastActive(currentTime);
        this.updateById(dbUser);

        return UserAuthVO.builder()
                .userId(dbUser.getId())
                .identifier(dbUser.getIdentifier())
                .type(dbUser.getType())
                .lastActive(dbUser.getLastActive())
                .needLogin(false)
                .build();
    }





    /**
     * 退出登录核心逻辑：清理 Redis 中的登录态缓存
     * @param token 用户登录时的令牌
     */
    @Override
    public void logout(String token) {
        // 1. 定义缓存键（与登录时一致）
        String userInfoKey = LOGIN_USER_TOKEN + token;

        // 2. 获取并解析用户信息，清理 userId 与 token 的映射
        String userInfoJson = redisTemplate.opsForValue().get(userInfoKey);
        if (userInfoJson != null) {
            // 直接解析并删除映射关系
            Long userId = JSONUtil.toBean(userInfoJson, RedisUserMsgDTO.class).getId();
            redisTemplate.delete(LOGIN_USER_TOKEN + userId);
        }

        // 3. 清除 token 对应的用户信息缓存
        redisTemplate.delete(userInfoKey);
    }


}