package com.resale.loveresaleappointment.components.customerAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoomInfoDTO {
    private Long customerId;
    private Integer userId;
    String customerName;
    String customerMobile;
    String customerMail;
    String customerFirebaseToken;
    String zoomUrl;
    Integer appointmentId;
}

