package com.example.demo.dto;

import lombok.Data;

import java.util.List;

/**
 * SQL处理器 DTO
 *
 * @author Eddy
 */
@Data
public class SqlProcessorDTO {

    /**
     * 执行SQL
     */
    private String sql;

    /**
     * 按列存储名称
     */
    private String resultVarName;

    /**
     * 按结果存储名称
     */
    private String resultContentName;

    /**
     * 提取参数列表
     */
    private List<ParamDTO> params;

}
