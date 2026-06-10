package com.ecommerceserver.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AIRspEnum {
    RECOMMENDATION("recommendation", "推荐"),
    SEARCHRESULT("search_result", "搜索结果"),
    IMAGE_SEARCH("image_search", "图片识别搜索");

    private final String code;
    private final String desc;

    public static AIRspEnum fromCode(String code) {
        for (AIRspEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}