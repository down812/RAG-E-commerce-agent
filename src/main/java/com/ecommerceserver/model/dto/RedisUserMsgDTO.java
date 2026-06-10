package com.ecommerceserver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author dawn
 * @date 2024-10-09 14:38
 * 缓存中存储用户数据（适配用户授权、登录状态管理）
 */
@Schema(description = "Redis缓存用户信息DTO")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class RedisUserMsgDTO {
    /**
     * 用户ID（与users表id一致，原字段名对齐，避免Bean拷贝异常）
     */
    @Schema(description = "用户ID", required = true)
    private Long id; //若users表id是Long类型，此处需改为Long

    /**
     * 原用户标识（临时用户：设备ID+时间戳；已登录用户：账号，对应UserServiceImpl中的identifier）
     */
    @Schema(description = "原用户标识（临时用户/已登录用户账号）", required = true)
    private String identifier; // 对齐UserServiceImpl中"原用户标识"逻辑

    /**
     * 账号（已登录用户专属，对应原account字段，临时用户可为空）
     */
    @Schema(description = "已登录用户账号（临时用户为空）")
    private String account; //区分临时用户与已登录用户的账号字段

    /**
     * 电话号码（用户授权时绑定，对应UserAuthDTO中的phone）
     */
    @Schema(description = "用户联系电话（授权后必传）")
    private String phone; // 对齐授权接口的手机号逻辑

    /**
     * 用户名（可选，完善用户信息时补充，非必传）
     */
    @Schema(description = "用户名（非必传）")
    private String userName;

    /**
     * 用户类型（0-超级管理员，1-普通管理员，2-普通用户，必传）
     */
    @Schema(description = "用户类型（0-超级管理员/1-普通管理员/2-普通用户）", required = true)
    private Integer type;


}