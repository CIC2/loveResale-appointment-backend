package com.resale.homeflyappointment.components.customerAppointment.dto.reschedule;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleAppointmentRequestDTO {
    private LocalDateTime schedule;
    private Integer appointmentId;
}


