package com.resale.homeflyappointment.components.internal;

import com.resale.homeflyappointment.repos.AppointmentRepository;
import com.resale.homeflyappointment.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InternalService {
    @Autowired
    AppointmentRepository appointmentRepository;

    public ReturnObject<Boolean> isUserOnCall(Integer userId) {

        boolean onCall =
                appointmentRepository.existsByUserIdAndStatus(userId, "ON_CALL");

        return new ReturnObject<>(
                "User on-call status retrieved",
                true,
                onCall
        );
    }
}


