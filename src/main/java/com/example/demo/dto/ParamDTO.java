package com.example.demo.dto;

import lombok.Data;

/**
 * 可变参数 DTO
 *
 * @author eddy
 */
@Data
public class ParamDTO {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数值
     */
    private String value;

    /**
     * 参数说明
     */
    private String description;

    /**
     * 是否有效
     */
    private Boolean enabled;

}
