package com.resale.homeflyappointment.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_exception_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentExceptionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer actionCode;
    private String actionName;
    private String identityType;
    private Integer identityId;
    private String httpMethod;
    private String exceptionType;

    @Column(columnDefinition = "TEXT")
    private String headers;

    @Column(columnDefinition = "TEXT")
    private String queryParams;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String stacktrace;

    private LocalDateTime createdAt = LocalDateTime.now();
}


