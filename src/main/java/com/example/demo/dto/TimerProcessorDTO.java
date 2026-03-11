package com.example.demo.dto;

import lombok.Data;

/**
 * 等待处理器DTO
 *
 * @author Eddy
 */
@Data
public class TimerProcessorDTO {

    /**
     * 等待时长（毫秒）
     */
    private Long waitTime;

}
