package com.resale.homeflyappointment.components.userAppointment.dto.scheduleAppointment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.resale.homeflyappointment.shared.FlexibleLocalDateTimeDeserializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserScheduleAppointmentRequestDTO {
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime schedule;
    private String userC4cId;
    private String customerSapPartnerId;
    private Long mobile;
    private String countryCode;
}


