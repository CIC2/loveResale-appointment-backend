package com.resale.loveresaleappointment.components.customerAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SingleSMSRequestDTO {
    String mobile;
    String content;
}


