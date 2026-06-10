package com.ecommerceserver.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductStatusEnum {
    UP(1, "上架"),
    DOWN(0, "下架");
    private Integer code;
    private String desc;
}
