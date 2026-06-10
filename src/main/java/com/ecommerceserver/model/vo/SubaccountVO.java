package com.ecommerceserver.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 子账号响应数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubaccountVO {

    @Schema(description = "子账号ID", required = true)
    private Long subaccountId;

    @Schema(description = "用户账号/临时标识", required = true)
    private String identifier;

    @Schema(description = "用户类型", required = true)
    private Integer type;

    @Schema(description = "创建时间", required = true)
    private Date createdAt;
}
