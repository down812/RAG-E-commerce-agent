package com.ecommerceserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "dataset")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSet implements Serializable {
    @TableId(value = "id",type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @TableField(value = "user_id")
    @Schema(description = "用户id")
    private Long userId;

    @TableField(value = "name")
    @Schema(description = "数据集名称")
    private String name;

    @TableField(value = "description")
    @Schema(description = "数据集描述")
    private String description;

    @TableField(value = "app_count")
    @Schema(description = "应用次数")
    private Integer appCount;

    @TableField(value = "doc_count")
    @Schema(description = "文档数")
    private Integer docCount;

    @TableField(value = "disabled")
    @Schema(description = "是否禁用")
    private Integer disabled;

    @TableField(value = "created_at")
    @Schema(description = "创建时间")
    private Date createdAt;

    @TableField(value = "updated_at")
    @Schema(description = "更新时间")
    private Date updatedAt;

    @TableField(value = "disabled_at")
    @Schema(description = "禁用时间")
    private Date disabledAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
