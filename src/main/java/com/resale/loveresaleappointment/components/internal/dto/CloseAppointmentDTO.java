package com.resale.loveresaleappointment.components.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloseAppointmentDTO {
    String appointmentId;
    String status;
}


