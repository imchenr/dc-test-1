package com.example.demo.dto;

import com.example.demo.enums.AssertActionEnum;
import com.example.demo.enums.AssertSourceEnum;
import lombok.Data;

/**
 * @author eddy
 */
@Data
public class AssertionDTO {

    /**
     * 断言名称
     */
    private String name;

    /**
     * 断言规则
     */
    private AssertActionEnum action;

    /**
     * 断言字段来源
     */
    private AssertSourceEnum source;

    /**
     * 断言目标值
     */
    private String value;

}

