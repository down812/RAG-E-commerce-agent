package com.ecommerceserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * @author dawn
 * @date 2024-09-10 15:50
 * 用户反馈表 user_feedback
 */
@TableName(value = "evaluate")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evaluate implements Serializable {
    //反馈表id
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    //用户id
    @TableField(value = "user_id")
    private Long userId;

    //会话id
    @TableField(value = "session_id")
    private String sessionId;

    //消息id
    @TableField(value = "message_id")
    private String messageId;

    //评价（点赞1、点踩-1，只有这2种值）
    @TableField(value = "rating")
    private Integer rating;

    //反馈内容
    @TableField(value = "comment")
    private String comment;

    //创建时间
    @TableField(value = "created_at")
    private String createdAt;

    //修改时间
    @TableField(value = "updated_at")
    private String updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
