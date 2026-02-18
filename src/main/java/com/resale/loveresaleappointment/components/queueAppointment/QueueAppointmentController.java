package com.resale.loveresaleappointment.components.queueAppointment;

import com.resale.loveresaleappointment.components.queueAppointment.dto.AppointmentInfoDTO;
import com.resale.loveresaleappointment.components.queueAppointment.dto.QueueAppointmentRequestDTO;
import com.resale.loveresaleappointment.components.queueAppointment.dto.SendToQueueRequest;
import com.resale.loveresaleappointment.components.userAppointment.UserAppointmentService;
import com.resale.loveresaleappointment.logging.LogActivity;
import com.resale.loveresaleappointment.model.ActionType;
import com.resale.loveresaleappointment.security.CheckPermission;
import com.resale.loveresaleappointment.security.JwtTokenUtil;
import com.resale.loveresaleappointment.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/queue")
public class QueueAppointmentController {
    @Autowired
    QueueAppointmentService queueAppointmentService;
    @Autowired
    UserAppointmentService userAppointmentService;
    @Autowired
    JwtTokenUtil JwtTokenUtil;

    @PostMapping("/internal/fromQueue")
    @LogActivity(ActionType.INTERNAL_CREATE_APPOINTMENT_FROM_QUEUE)
    public ResponseEntity<?> createAppointmentFromQueue(
            @RequestBody QueueAppointmentRequestDTO dto
    ) {
        return queueAppointmentService.createAppointmentFromQueue(dto);
    }

    @PostMapping("/internal/endCall")
    @LogActivity(ActionType.INTERNAL_END_CALL_FROM_QUEUE)
    public ResponseEntity<?> endCallFromQueue(
            @RequestParam Integer salesmanId
    ) {
        return queueAppointmentService.endCallFromQueue(salesmanId);
    }

    @PutMapping("/internal/changeStatus/{appointmentId}/{userId}")
    @LogActivity(ActionType.INTERNAL_CHANGE_APPOINTMENT_STATUS_FROM_QUEUE)
    public ResponseEntity<?> changeStatusUserAppointmentFromQueue(
            @PathVariable Integer appointmentId,
            @PathVariable Integer userId
    ) {
        return userAppointmentService.changeStatusUserAppointment(
                userId,
                appointmentId
        );
    }


    @PostMapping("/sendToQueue")
    @CheckPermission(value = {"sales:login"})
    @LogActivity(ActionType.SEND_APPOINTMENT_TO_QUEUE)
    public ResponseEntity<ReturnObject<String>> sendAppointmentToQueue(
            @RequestBody SendToQueueRequest request) {

        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!(principal instanceof Jwt jwt)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ReturnObject<>("Unauthorized", false, null));
            }

            Integer userId = JwtTokenUtil.extractUserId(jwt.getTokenValue());

            ReturnObject<AppointmentInfoDTO> appointmentResult =
                    queueAppointmentService.getAppointmentInfoById(request.getAppointmentId());

            if (!appointmentResult.getStatus()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ReturnObject<>("Appointment not found", false, null));
            }

            ReturnObject<String> queueResult =
                    queueAppointmentService.sendAppointmentToQueue(appointmentResult.getData(), userId);

            HttpStatus status = queueResult.getStatus() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(queueResult);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReturnObject<>("Failed to process request: " + ex.getMessage(), false, null));
        }
    }
}

