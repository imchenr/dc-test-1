package com.example.demo.dto;

import com.example.demo.enums.ScriptLanguageEnum;
import lombok.Data;

/**
 * 脚本处理器DTO
 *
 * @author Eddy
 */
@Data
public class ScriptProcessorDTO {

    /**
     * 脚本语言
     */
    private ScriptLanguageEnum language;

    /**
     * 内容
     */
    private String content;

}
