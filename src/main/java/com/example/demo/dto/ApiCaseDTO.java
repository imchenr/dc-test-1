package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * API测试用例 DTO
 *
 * @author eddy
 */
@Getter
@Setter
public class ApiCaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 用例名称
     */
    private String name;

    /**
     * 项目、环境映射关系，key为项目id，value为环境id
     */
    private Map<Long, Long> projectEnvMap;

    /**
     * 全局可变参数
     */
    private List<ParamDTO> relation;

    /**
     * 接口测试步骤
     */
    private List<ApiCaseStepDTO> steps;

}
