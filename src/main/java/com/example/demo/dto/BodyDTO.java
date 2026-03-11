package com.example.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class BodyDTO {

    private String type;

    private String content;

    private List<ParamDTO> list;

}
