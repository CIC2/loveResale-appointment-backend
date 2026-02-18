package com.resale.loveresaleappointment.components.userAppointment.dto.scheduleAppointment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.resale.loveresaleappointment.shared.FlexibleLocalDateTimeDeserializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserScheduleAppointmentRequestDTO {
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime schedule;
    private Long mobile;
    private String countryCode;
}


