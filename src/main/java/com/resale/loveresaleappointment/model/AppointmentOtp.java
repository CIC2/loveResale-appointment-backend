package com.resale.loveresaleappointment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Long customerId;
    private String otp;
    private Boolean isVerified;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;


    public AppointmentOtp(
            Long customerId,
            String otp,
            Boolean isVerified,
            LocalDateTime expiresAt,
            LocalDateTime createdAt
    ) {
        this.customerId = customerId;
        this.otp = otp;
        this.isVerified = isVerified;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }
}



