package com.ecommerceserver.model.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Pattern;

/**
 * 修改子账号请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubaccountUpdateDTO {

    @Schema(description = "新密码（可选）")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,20}$", message = "密码需为8-20位，包含大小写字母和数字")
    private String password;

    @Schema(description = "新类型（可选，仅支持1↔2互转，禁止修改为0）")
    @Pattern(regexp = "^(1|2)$", message = "修改后类型仅支持 1(普通管理员)、2(普通用户)")
    private Integer type;
}
