package com.ecommerceserver.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserEnum {
    SUPER_ADMIN(0, "超级管理员"),
    ADMIN(1, "普通管理员"),
    USER(2, "普通用户");

    private Integer code;
    private String desc;
}