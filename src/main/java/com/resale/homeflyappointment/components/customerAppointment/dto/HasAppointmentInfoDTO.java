package com.resale.homeflyappointment.components.customerAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HasAppointmentInfoDTO {
    Boolean hasAppointment ;
    Integer appointmentId;
    Boolean isRateEmpty;
}

