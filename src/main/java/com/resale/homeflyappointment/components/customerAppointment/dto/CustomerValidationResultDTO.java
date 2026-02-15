package com.resale.homeflyappointment.components.customerAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerValidationResultDTO {
    private boolean valid;
    private List<String> missingFields;
}

