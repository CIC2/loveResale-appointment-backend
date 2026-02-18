package com.resale.loveresaleappointment.components.customerAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RatingDTO {
    Integer rate1;
    Integer rate2;
    Integer rate3;
    Integer rate4;
    String comment;
}


