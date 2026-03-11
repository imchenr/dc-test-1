package com.example.demo.dto;

import com.example.demo.enums.ScopeTypeEnum;
import lombok.Data;

import java.util.List;

@Data
public class EnvironmentHttpInfoDTO {

    /**
     * 协议
     */
    private String protocol;

    /**
     * 域名
     */
    private String domain;

    /**
     * 描述
     */
    private String description;

    /**
     * 作用域
     */
    private ScopeTypeEnum scopeType;

    /**
     * 作用域条件
     */
    private String scope;

    /**
     * 请求头
     */
    private List<ParamDTO> headers;

}
