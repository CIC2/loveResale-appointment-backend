package com.resale.homeflyappointment.components.userAppointment.dto.scheduleAppointment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserScheduleAppointmentSapResponse {
    @JsonProperty("ID")
    private String appointmentId;
}


