package com.resale.homeflyappointment.components.customerAppointment.dto.scheduleAppointment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScheduleAppointmentSapResponse {
    @JsonProperty("ID")
    private String appointmentId;
    @JsonProperty("OwnerPartyID")
    private String ownerPartyId;
}


