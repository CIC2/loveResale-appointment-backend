package com.resale.loveresaleappointment.components.teamLeadAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class AppointmentTeamLeadDTO {
    private Integer appointmentId;
    private Timestamp appointmentDate;
    private String appointmentStatus;
    private Integer userId;
    private CustomerBasicInfoDTO customer;
}


