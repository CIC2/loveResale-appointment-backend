package com.resale.homeflyappointment.repos;

import com.resale.homeflyappointment.model.AppointmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AppointmentLogRepository extends JpaRepository<AppointmentLog, Integer> {

}

