package com.resale.loveresaleappointment.repos;

import com.resale.loveresaleappointment.model.AppointmentExceptionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentExceptionLogRepository extends JpaRepository<AppointmentExceptionLog, Integer> {

}


