package com.resale.loveresaleappointment.components.teamLeadAppointment;

import com.resale.loveresaleappointment.components.teamLeadAppointment.dto.AppointmentTeamLeadDTO;
import com.resale.loveresaleappointment.components.teamLeadAppointment.dto.CustomerBasicInfoDTO;
import com.resale.loveresaleappointment.components.userAppointment.dto.rescheduleAppointment.UserRescheduleAppointmentRequestDTO;
import com.resale.loveresaleappointment.feign.CustomerMSFeignClient;
import com.resale.loveresaleappointment.feign.UserFeignClient;
import com.resale.loveresaleappointment.model.Appointment;
import com.resale.loveresaleappointment.repos.AppointmentRepository;
import com.resale.loveresaleappointment.security.JwtTokenUtil;
import com.resale.loveresaleappointment.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamLeadService {

    private final AppointmentRepository appointmentRepository;
    private final UserFeignClient userFeignClient;
    private final JwtTokenUtil jwtTokenUtil;
    private final CustomerMSFeignClient customerMSFeignClient;


    public ReturnObject<List<AppointmentTeamLeadDTO>> getAppointmentsByUserId(
            Integer userId,
            String status,
            Integer teamLeadId,
            String token
    ) {

        String roles = jwtTokenUtil.extractRole(token);
        if (roles == null || !roles.contains("TEAM_LEAD")) {
            return new ReturnObject<>("Access denied", false, null);
        }

        ResponseEntity<ReturnObject> assignedResponse =
                userFeignClient.isUserAssignedToTeamLead(teamLeadId, userId);

        if (assignedResponse.getBody() == null
                || Boolean.FALSE.equals(assignedResponse.getBody().getStatus())) {
            return new ReturnObject<>("User not assigned to this team lead", false, null);
        }

        List<Appointment> appointments;

        if (status == null || status.trim().isEmpty()) {
            appointments = appointmentRepository.findAllByUserId(userId);
        } else {
            appointments = appointmentRepository.findAllByUserIdAndStatus(Long.valueOf(userId), status);
        }

        if (appointments.isEmpty()) {
            return new ReturnObject<>("No appointments found", true, Collections.emptyList());
        }

        List<Long> customerIds = appointments.stream()
                .map(Appointment::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        ReturnObject<List<CustomerBasicInfoDTO>> customersResponse =
                customerMSFeignClient.getCustomersBasicInfo(customerIds);

        List<CustomerBasicInfoDTO> customers =
                (customersResponse != null && Boolean.TRUE.equals(customersResponse.getStatus()))
                        ? customersResponse.getData()
                        : Collections.emptyList();

        Map<Long, CustomerBasicInfoDTO> customerMap =
                customers.stream()
                        .filter(c -> c.getCustomerId() != null)
                        .collect(Collectors.toMap(
                                c -> c.getCustomerId().longValue(),
                                c -> c
                        ));

        List<AppointmentTeamLeadDTO> result = appointments.stream()
                .sorted(Comparator.comparing(Appointment::getAppointmentDate).reversed())
                .map(a -> new AppointmentTeamLeadDTO(
                        a.getId(),
                        a.getAppointmentDate(),
                        a.getStatus(),
                        a.getUserId(),
                        customerMap.get(a.getCustomerId())
                ))
                .toList();

        return new ReturnObject<>("Success", true, result);
    }


    public ResponseEntity<?> rescheduleUserAppointmentByTeamLead(
            Integer userId,
            Integer salesmanId,
            UserRescheduleAppointmentRequestDTO req
    ) {

        System.out.println("---- [Reschedule Appointment] START ----");
        System.out.println("Incoming userId: " + salesmanId);
        System.out.println("Incoming appId: " + req.getAppId());
        System.out.println("Incoming new schedule: " + req.getSchedule());

        ResponseEntity<ReturnObject> assignedResponse =
                userFeignClient.isUserAssignedToTeamLead(userId, salesmanId);

        if (assignedResponse.getBody() == null
                || Boolean.FALSE.equals(assignedResponse.getBody().getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ReturnObject<>("User not assigned to this team lead", false, null)
            );
        }

        // ---------------------------------------------------------
        // 1️⃣ Fetch Existing Appointment
        // ---------------------------------------------------------
        Optional<Appointment> optAppointment =
                appointmentRepository.getAppointmentByUserIdAndIdAndStatus(
                        salesmanId,
                        Integer.valueOf(req.getAppId()),
                        "OPEN"
                );

        if (optAppointment.isEmpty()) {
            System.out.println("❌ No appointment found to reschedule");
            return ResponseEntity.ok(
                    new ReturnObject<>(
                            "No appointments found for this user with this Id",
                            false,
                            Collections.emptyList()
                    )
            );
        }

        Appointment oldAppointment = optAppointment.get();

        // ---------------------------------------------------------
        // 2️⃣ Close Old Appointment
        // ---------------------------------------------------------
        oldAppointment.setStatus("RESCHEDULED");

        try {
            appointmentRepository.save(oldAppointment);
            System.out.println("Old appointment updated -> status = RESCHEDULED");
        } catch (Exception ex) {
            System.out.println("❌ Failed to update old appointment: " + ex.getMessage());
        }

        // ---------------------------------------------------------
        // 3️⃣ Create New Appointment
        // ---------------------------------------------------------
        Appointment newAppointment = new Appointment();
        newAppointment.setUserId(oldAppointment.getUserId());
        newAppointment.setCustomerId(oldAppointment.getCustomerId());
        newAppointment.setType("SALES RESCHEDULED");
        newAppointment.setStatus("OPEN");

        try {
            newAppointment.setAppointmentDate(Timestamp.valueOf(req.getSchedule()));
        } catch (Exception ex) {
            System.out.println("⚠️ Invalid date in request: " + ex.getMessage());
        }

        // ---------------------------------------------------------
        // 4️⃣ Save New Appointment Locally
        // ---------------------------------------------------------
        try {
            appointmentRepository.save(newAppointment);
            System.out.println("New appointment saved ID = " + newAppointment.getId());
        } catch (Exception ex) {
            System.out.println("❌ Failed to save new appointment: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Failed to create rescheduled appointment locally", false, null)
            );
        }

        // ---------------------------------------------------------
        // 5️⃣ Final Response
        // ---------------------------------------------------------
        System.out.println("---- [Reschedule Appointment] END SUCCESS ----");

        return ResponseEntity.ok(
                new ReturnObject<>(
                        "Appointment rescheduled successfully",
                        true,
                        newAppointment
                )
        );
    }

    public ReturnObject<Appointment> reassignAppointment(int appointmentId, int newUserId, int teamLeadId) {
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(appointmentId);

        if (optionalAppointment.isEmpty()) {
            return new ReturnObject<>("Appointment not found", false, null);
        }

        Appointment appointment = optionalAppointment.get();

        ResponseEntity<ReturnObject> assignedResponse =
                userFeignClient.isUserAssignedToTeamLead(teamLeadId, appointment.getUserId());

        if (assignedResponse.getBody().getStatus().equals(false)) {
            return new ReturnObject<>("User not assigned to this team lead", false, null);
        }

        appointment.setUserId(newUserId);
        try {
            Appointment updated = appointmentRepository.save(appointment);
            return new ReturnObject<>("Appointment reassigned successfully", true, updated);
        } catch (Exception e) {
            return new ReturnObject<>("Failed to reassign appointment. The user may not exist or is invalid.", false, null);
        }
    }
}