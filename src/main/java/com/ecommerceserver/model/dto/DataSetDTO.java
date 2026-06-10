package com.ecommerceserver.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSetDTO {
    @TableField(value = "name")
    @Schema(description = "数据集名称")
    private String name;

    @TableField(value = "description")
    @Schema(description = "数据集描述")
    private String description;

    @TableField(value = "disabled")
    @Schema(description = "是否禁用")
    private Integer disabled;
}
