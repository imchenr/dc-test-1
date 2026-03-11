package com.example.demo.dto;

import com.example.demo.enums.ProcessorTypeEnum;
import lombok.Data;

/**
 * 前后置操作处理器DTO
 *
 * @author Eddy
 */
@Data
public class ProcessorDTO {

    /**
     * 处理器类型
     */
    private ProcessorTypeEnum processorType;

    /**
     * 脚本处理器详情（processorType为SCRIPT时生效）
     */
    private ScriptProcessorDTO script;

    /**
     * 等待处理器详情（processorType为TIMER时生效）
     */
    private TimerProcessorDTO timer;

    /**
     * SQL处理器详情（processorType为SQL时生效）
     */
    private SqlProcessorDTO sql;

}
