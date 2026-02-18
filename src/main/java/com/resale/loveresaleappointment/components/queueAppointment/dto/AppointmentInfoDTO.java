package com.resale.loveresaleappointment.components.queueAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppointmentInfoDTO {
    private Integer appointmentId;
    private Integer salesmanId;
    private Long customerId;
}


