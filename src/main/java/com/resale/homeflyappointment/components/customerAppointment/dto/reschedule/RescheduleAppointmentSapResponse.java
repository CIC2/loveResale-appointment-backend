package com.resale.homeflyappointment.components.customerAppointment.dto.reschedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RescheduleAppointmentSapResponse {
    @JsonProperty("ID")
    private String appointmentId;
}


