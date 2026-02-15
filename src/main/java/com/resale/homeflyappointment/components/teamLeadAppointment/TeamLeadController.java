package com.resale.homeflyappointment.components.teamLeadAppointment;

import com.resale.homeflyappointment.components.teamLeadAppointment.dto.AppointmentTeamLeadDTO;
import com.resale.homeflyappointment.components.userAppointment.UserAppointmentService;
import com.resale.homeflyappointment.components.userAppointment.dto.rescheduleAppointment.UserRescheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.logging.LogActivity;
import com.resale.homeflyappointment.model.ActionType;
import com.resale.homeflyappointment.model.Appointment;
import com.resale.homeflyappointment.security.CookieBearerTokenResolver;
import com.resale.homeflyappointment.security.JwtTokenUtil;
import com.resale.homeflyappointment.security.user.CurrentUserId;
import com.resale.homeflyappointment.utils.ReturnObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teamLead")
public class TeamLeadController {
    @Autowired
    TeamLeadService teamLeadService;
    @Autowired
    CookieBearerTokenResolver tokenResolver;
    @Autowired
    JwtTokenUtil jwtTokenUtil;

    @GetMapping("/user/{userId}")
    @LogActivity(ActionType.GET_APPOINTMENTS_BY_USER_ID)
    public ResponseEntity<ReturnObject<List<AppointmentTeamLeadDTO>>> getAppointmentsByUserId(
            @PathVariable Integer userId,
            @RequestParam (value =  "status",required = false) String status,
            HttpServletRequest request
    ) {
        String token = tokenResolver.resolve(request);

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ReturnObject<>("Unauthorized", false, null));
        }

        Integer teamLeadId = jwtTokenUtil.extractUserId(token);

        ReturnObject<List<AppointmentTeamLeadDTO>> response =
                teamLeadService.getAppointmentsByUserId(userId,status, teamLeadId, token);

        if (!response.getStatus()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/rescheduleByTeamLead")
    @LogActivity(ActionType.TEAM_LEAD_RESCHEDULE_APPOINTMENT)
    public ResponseEntity<?> rescheduleUserAppointmentsByTeamLead(@CurrentUserId Long userId,@RequestParam Long salesmanId, @RequestBody UserRescheduleAppointmentRequestDTO rescheduleAppointmentRequestDTO) {
        return teamLeadService.rescheduleUserAppointmentByTeamLead(Math.toIntExact(userId),Math.toIntExact(salesmanId), rescheduleAppointmentRequestDTO);
    }

    @PostMapping("/reassign")
    @LogActivity(ActionType.REASSIGN_APPOINTMENT)
    public ResponseEntity<ReturnObject<Appointment>> reassignAppointment(
            HttpServletRequest request,
            @RequestParam int appointmentId,
            @RequestParam int newUserId
    ) {
        try {
            String token = tokenResolver.resolve(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ReturnObject<>("Missing token", false, null));
            }

            Integer teamLeadId = jwtTokenUtil.extractUserId(token);

            ReturnObject<Appointment> result = teamLeadService.reassignAppointment(
                    appointmentId,
                    newUserId,
                    teamLeadId
            );

            if (!result.getStatus()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
            }

            return ResponseEntity.status(HttpStatus.OK).body(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ReturnObject<>("Invalid token: " + e.getMessage(), false, null));
        }
    }
}


