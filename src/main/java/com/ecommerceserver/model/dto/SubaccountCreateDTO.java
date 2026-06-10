package com.ecommerceserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 创建子账号请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubaccountCreateDTO {
    // 账号：非空，建议限制格式（如字母/数字，长度6-20）
    @NotBlank(message = "用户账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "账号需为6-20位字母或数字")
    private String identifier;

    // 密码：非空，建议限制强度（如包含大小写+数字，长度8-20）
    @NotBlank(message = "用户密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,20}$", message = "密码需为8-20位，包含大小写字母和数字")
    private String password;

    // 用户类型：非空，仅允许 0(超级管理员)、1(普通管理员)、2(普通用户)
    @NotNull(message = "用户类型不能为空")
    @Pattern(regexp = "^(0|1|2)$", message = "用户类型仅支持 0(超级管理员)、1(普通管理员)、2(普通用户)")
    private Integer type;
}
