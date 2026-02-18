package com.resale.loveresaleappointment.components.userAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelProjectDTO {
    private String modelName;
    private String modelCode;
    private Integer projectId;
    private String projectName;
}


