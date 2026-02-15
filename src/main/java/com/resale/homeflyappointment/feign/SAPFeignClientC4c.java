package com.resale.homeflyappointment.feign;

import com.resale.homeflyappointment.components.customerAppointment.dto.availableSlots.AppointmentSlotsResponseDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.scheduleAppointment.ScheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.scheduleAppointment.ScheduleAppointmentSapResponse;
import com.resale.homeflyappointment.components.customerAppointment.dto.reschedule.RescheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.reschedule.RescheduleAppointmentSapResponse;
import com.resale.homeflyappointment.components.userAppointment.dto.cancelAppointment.C4cCancelAppointmentDto;
import com.resale.homeflyappointment.components.userAppointment.dto.scheduleAppointment.UserScheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.components.userAppointment.dto.scheduleAppointment.UserScheduleAppointmentSapResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "sap-appointments", url = "${sap.appointments.url}")
public interface SAPFeignClientC4c {

    @GetMapping("/availableSlots")
    ResponseEntity<AppointmentSlotsResponseDTO> getAvailableSlots();

    @PostMapping("/customerScheduleAppointment")
    ResponseEntity<ScheduleAppointmentSapResponse> customerScheduleAppointment(@RequestBody ScheduleAppointmentRequestDTO request);
    @PostMapping("/customerRescheduleAppointment")
    ResponseEntity<RescheduleAppointmentSapResponse> customerRescheduleAppointment(@RequestBody RescheduleAppointmentRequestDTO request);
    @PostMapping("/userScheduleAppointment")
    ResponseEntity<UserScheduleAppointmentSapResponse> userScheduleAppointment(@RequestBody UserScheduleAppointmentRequestDTO request);
    @PostMapping("/cancelAppointment")
    ResponseEntity<?> userCancelAppointment(@RequestBody C4cCancelAppointmentDto c4cCancelAppointmentDto);

}


