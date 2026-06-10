package com.ecommerceserver.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EvaluateEnum {
    LIKE(1,"点赞"),
    DISLIKE(-1,"点踩");

    private Integer code;
    private String desc;
}
