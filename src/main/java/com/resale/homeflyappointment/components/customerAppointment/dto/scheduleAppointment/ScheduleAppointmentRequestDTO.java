package com.resale.homeflyappointment.components.customerAppointment.dto.scheduleAppointment;

import lombok.Data;

@Data
public class ScheduleAppointmentRequestDTO {
    private String schedule;
    private String sapPartnerId;
    private Integer modelId;
    private Integer projectId;
}


