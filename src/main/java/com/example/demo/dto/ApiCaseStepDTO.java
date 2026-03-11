package com.example.demo.dto;

import lombok.Data;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * 接口测试步骤 DTO
 *
 * @author eddy
 */
@Data
public abstract class ApiCaseStepDTO {

    private Long projectId;

    private Long environmentId;

    /**
     * 步骤名称
     */
    private String name;

}
