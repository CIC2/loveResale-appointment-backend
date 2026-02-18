package com.resale.loveresaleappointment.components.queueAppointment.dto;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class QueueAppointmentRequestDTO {
    private Integer salesmanId;
    private Long customerId;
    private String mobile;
    private String callId;
    private Timestamp callStartTime;
    private Timestamp appointmentDate;
    private Integer modelId;
}


