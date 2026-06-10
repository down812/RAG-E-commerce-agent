package com.ecommerceserver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthDTO {


    @Schema(description = "原用户标识（免登录用户的guest_xxx / 老用户的旧用户名）", required = true)
    @NotBlank(message = "原用户标识不能为空")
    private String oldIdentifier; // 必传：区分免登录用户/老用户的原始标识

    @Schema(description = "新用户标识（用户要设置的登录用户名）", required = true)
    @NotBlank(message = "新用户标识不能为空")
    private String newIdentifier; // 必传：用户想要设置的新用户名

    @Schema(description = "用户联系邮箱", required = true)
    @NotBlank(message = "联系邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$", message = "邮箱格式不正确")
    private String email;

    @Schema(description = "用户密码（8-16位，包含字母和数字）", required = true)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$", message = "密码需8-16位，包含字母和数字")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "验证码", required = true)
    @NotBlank(message = "验证码不能为空")
    private String code;
}
