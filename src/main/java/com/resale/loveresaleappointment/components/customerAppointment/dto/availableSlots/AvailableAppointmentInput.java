package com.resale.loveresaleappointment.components.customerAppointment.dto.availableSlots;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AvailableAppointmentInput {

    @JsonProperty("Booked")
    private BookInput book;

}


