package com.resale.homeflyappointment.components.customerAppointment.dto;

import lombok.Data;

@Data
public class ProfileResponseDTO {
    private String sapPartnerId;
    private Long customerId;
    private String fullName;
    private String mobile;
    private String countryCode;
    private String fcmToken;
}


