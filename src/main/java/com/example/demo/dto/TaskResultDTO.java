package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试任务结果 DTO
 *
 * @author eddy
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResultDTO {

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 结果
     */
    private String result;

}
