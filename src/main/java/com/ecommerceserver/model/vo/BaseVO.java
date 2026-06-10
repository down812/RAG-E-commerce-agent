package com.ecommerceserver.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@AllArgsConstructor
@SuperBuilder
@Data
@NoArgsConstructor
public class BaseVO {
    private String sessionId;
    private String messageId;
    private String responseType;
    private String answer;
    private List<Source> sources;
    private Long timestamp;
}
