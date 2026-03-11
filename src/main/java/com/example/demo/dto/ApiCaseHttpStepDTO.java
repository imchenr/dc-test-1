package com.example.demo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpMethod;

import java.io.Serializable;
import java.util.List;

/**
 * API HTTP请求步骤DTO
 *
 * @author eddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiCaseHttpStepDTO extends ApiCaseStepDTO implements Serializable {

    /**
     * 请求方法 [GET POST PUT PATCH DELETE HEAD OPTIONS]
     */
    private HttpMethod method;

    /**
     * 接口路径
     */
    private String path;

    /**
     * 响应断言
     */
    private List<AssertionDTO> assertion;

    /**
     * 查询参数
     */
    private List<ParamDTO> query;

    /**
     * 请求头
     */
    private List<ParamDTO> header;

    /**
     * 请求体
     */
    private BodyDTO body;

    /**
     * 前置操作
     */
    private List<ProcessorDTO> preProcessor;

    /**
     * 后置操作
     */
    private List<ProcessorDTO> postProcessor;

}
