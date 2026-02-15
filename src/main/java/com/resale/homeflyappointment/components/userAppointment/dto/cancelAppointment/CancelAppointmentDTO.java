package com.resale.homeflyappointment.components.userAppointment.dto.cancelAppointment;

import lombok.Data;

@Data
public class CancelAppointmentDTO {

    private Integer appId;
    private String note;
    private String reasonCode;
}


