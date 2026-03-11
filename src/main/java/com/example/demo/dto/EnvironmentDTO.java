package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class EnvironmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 环境名称
     */
    private String name;

    /**
     * HTTP请求信息
     */
    private List<EnvironmentHttpInfoDTO> httpInfos;

}
