package com.resale.homeflyappointment.components.customerAppointment.dto;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class AppointmentDto {
    private Integer appointmentId;
    private String userFullName;
    private Timestamp appointmentDateTime;
    private String status;

    public AppointmentDto(Integer appointmentId, String userFullName, Timestamp appointmentDateTime,String status) {
        this.appointmentId = appointmentId;
        this.userFullName = userFullName;
        this.appointmentDateTime = appointmentDateTime;
        this.status = status;
    }
}


