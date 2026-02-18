package com.resale.loveresaleappointment.repos;

import com.resale.loveresaleappointment.model.AppointmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AppointmentLogRepository extends JpaRepository<AppointmentLog, Integer> {

}

