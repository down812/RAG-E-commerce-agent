package com.ecommerceserver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询子账号请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubaccountQueryDTO {

    @Schema(description = "用户账号（可选）")
    private String identifier;

    @Schema(description = "子账号类型：0-超级管理员，1-普通管理员，2-普通用户（可选）")
    private Integer type;


}
