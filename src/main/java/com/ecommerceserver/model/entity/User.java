package com.ecommerceserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;


@TableName(value = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    @TableId(value = "id",type = IdType.AUTO)
    private Long id ;


    @Schema(description = "用户临时标识或者用户账号")
    @TableField(value = "identifier")
    private String identifier;

    @Schema(description = "用户密码")
    @TableField(value = "password")
    private String password;


    @Schema(description = "用户联系电话")
    @TableField(value = "phone")
    private String phone;


    @Schema(description = "用户类型, 0-超级管理员，1-普通管理员，2-普通用户")
    @TableField(value = "type")
    private Integer type;


    @Schema(description = "创建时间")
    @TableField(value = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;


    @Schema(description = "更新时间")
    @TableField(value = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;


    @Schema(description = "用户最近一次活跃时间")
    @TableField(value = "last_active")
    private Date lastActive;

    @Schema(description = "用户联系邮箱")
    @TableField(value = "email")
    private String email;


}
