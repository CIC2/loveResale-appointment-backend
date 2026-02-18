package com.resale.loveresaleappointment.repos;

import com.resale.loveresaleappointment.model.AppointmentOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentOtpRepository extends JpaRepository<AppointmentOtp, Integer> {
    AppointmentOtp findTopByCustomerIdOrderByCreatedAtDesc(Long customerId);
    }


