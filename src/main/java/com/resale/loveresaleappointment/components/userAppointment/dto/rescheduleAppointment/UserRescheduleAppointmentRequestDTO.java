package com.resale.loveresaleappointment.components.userAppointment.dto.rescheduleAppointment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.resale.loveresaleappointment.shared.FlexibleLocalDateTimeDeserializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRescheduleAppointmentRequestDTO {
    private Integer appId;
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime schedule;
    private String note;
    private String reasonCode;
}


