package com.resale.loveresaleappointment.components.userAppointment.dto;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class UserAppointmentDto {
    private Integer appointmentId;
    private Long customerId;
    private String customerFullName;
    private String countryCode;
    private String customerMobile;
    private String status;
    private Timestamp appointmentDateTime;

    public UserAppointmentDto(Integer appointmentId,Long customerId,String status,
                              String customerFullName, String countryCode,String customerMobile,
                              Timestamp appointmentDateTime) {
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.customerFullName = customerFullName;
        this.countryCode = countryCode;
        this.customerMobile = customerMobile;
        this.status = status;
        this.appointmentDateTime = appointmentDateTime;
    }
}


