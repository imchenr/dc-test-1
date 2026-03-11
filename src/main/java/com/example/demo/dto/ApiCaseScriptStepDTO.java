package com.example.demo.dto;

import com.example.demo.enums.ScriptLanguageEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class ApiCaseScriptStepDTO extends ApiCaseStepDTO implements Serializable {

    /**
     * 脚本语言
     */
    private ScriptLanguageEnum language;

    /**
     * 内容
     */
    private String content;

}
