package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportDTO {

    private Long id;

    private Long projectId;

    private Long caseId;

    private String type;

    private String name;

    private String executeState;

    private String summary;

    private Long startTime;

    private Long endTime;

    private Long expandTime;

    private Long quantity;

    private Long passQuantity;

    private Long failQuantity;

}
