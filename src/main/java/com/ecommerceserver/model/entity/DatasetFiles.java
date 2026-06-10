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

@TableName(value = "dataset_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetFiles implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "name")
    @Schema(description = "文件名")
    private String name;

    @TableField(value = "file_path")
    @Schema(description = "文件路径")
    private String filePath;

    @TableField(value = "file_type")
    @Schema(description = "文件类型")
    private String fileType;

    @TableField(value = "file_size")
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @TableField(value = "dataset_id")
    @Schema(description = "所属数据集ID")
    private Long datasetId;

    @TableField(value = "user_id")
    @Schema(description = "上传用户ID")
    private Long userId;

    @TableField(value = "disabled")
    @Schema(description = "是否禁用：1-启用，-1-禁用")
    private Integer disabled;

    @TableField(value = "created_at")
    @Schema(description = "创建时间")
    private Date createdAt;

    @TableField(value = "disabled_at")
    @Schema(description = "禁用时间")
    private Date disabledAt;

    @TableField(value = "hit_count")
    @Schema(description = "命中次数")
    private Integer hitCount;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}