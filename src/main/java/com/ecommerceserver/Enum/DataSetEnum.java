package com.ecommerceserver.Enum;

import com.ecommerceserver.model.dto.DataSetDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataSetEnum {
    ON(1, "启用"),
    OFF(-1, "禁用");

    private Integer code;
    private String desc;
}
