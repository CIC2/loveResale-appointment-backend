package com.resale.loveresaleappointment.components.customerAppointment.dto.availableSlots;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BookInput {
    @JsonProperty("startDate")
    private String startDate;
    @JsonProperty("slots")
    private String slots;

}


