package com.ecommerceserver.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户授权信息响应数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuthVO {

    @Schema(description = "用户ID",required = true)
    private Long userId;


    @Schema(description = "用户标识", required = true)
    private String identifier;


    @Schema(description = "用户类型（0-超级管理员，1-普通管理员，2-普通用户）",
            required = true)
    private Integer type;

    @Schema(description = "用户上次活跃时间" , required = true)
    private Date lastActive;

    @Schema(description = "是否需要重新登录", required = true)
    private Boolean needLogin;



}
