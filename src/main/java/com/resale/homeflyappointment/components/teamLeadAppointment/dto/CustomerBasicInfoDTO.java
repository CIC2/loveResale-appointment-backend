package com.resale.homeflyappointment.components.teamLeadAppointment.dto;

import lombok.Data;

@Data
public class CustomerBasicInfoDTO {
    private Integer customerId;
    private String fullName;
    private String mobile;
    private String countryCode;
    private String email;


    public CustomerBasicInfoDTO(
            Integer customerId,
            String fullName,
            String mobile
    ) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.mobile = mobile;
    }
}


