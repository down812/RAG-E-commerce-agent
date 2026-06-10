package com.ecommerceserver.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SourceEnum {
    DATABASE("database", "数据库"),
    RAG("rag", "RAG");

    private String sourceType;
    private String desc;
}
