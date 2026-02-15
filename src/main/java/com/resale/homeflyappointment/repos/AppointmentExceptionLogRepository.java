package com.resale.homeflyappointment.repos;

import com.resale.homeflyappointment.model.AppointmentExceptionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentExceptionLogRepository extends JpaRepository<AppointmentExceptionLog, Integer> {

}


