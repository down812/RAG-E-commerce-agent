package com.ecommerceserver.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginVO {

    @Schema(description = "登录令牌", required = true)
    private String token;

    @Schema(description = "用户ID", required = true)
    private Long userId;

    @Schema(description = "用户类型", required = true)
    private Integer type;

    @Schema(description = "用户标识", required = true)
    private String identifier;

}
