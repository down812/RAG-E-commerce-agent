package com.ecommerceserver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginDTO {

    @NotBlank(message = "登录账号不能为空")
    @Schema(description = "登录账号", required = true)
    private String identifier;

    @NotBlank(message = "登录密码不能为空")
    @Schema(description = "登录密码", required = true)
    private String password;


}
