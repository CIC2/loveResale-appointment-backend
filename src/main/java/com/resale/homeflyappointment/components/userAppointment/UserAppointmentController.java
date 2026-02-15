package com.resale.homeflyappointment.components.userAppointment;

import com.resale.homeflyappointment.components.userAppointment.dto.cancelAppointment.CancelAppointmentDTO;
import com.resale.homeflyappointment.components.userAppointment.dto.rescheduleAppointment.UserRescheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.components.userAppointment.dto.scheduleAppointment.UserScheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.logging.LogActivity;
import com.resale.homeflyappointment.model.ActionType;
import com.resale.homeflyappointment.security.CheckPermission;
import com.resale.homeflyappointment.security.JwtTokenUtil;
import com.resale.homeflyappointment.security.user.CurrentUserId;
import com.resale.homeflyappointment.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserAppointmentController {
    @Autowired
    private UserAppointmentService userAppointmentService;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    @GetMapping("")
    @LogActivity(ActionType.USER_GET_APPOINTMENTS)
    public ResponseEntity<?> getUserAppointments(
            @CurrentUserId Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "mobile", required = false) Long mobile,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate
    ) {
        return userAppointmentService.getUserAppointment(
                userId, name, mobile, fromDate, toDate, PageRequest.of(page, size)
        );
    }


    @PostMapping("")
    @LogActivity(ActionType.USER_CREATE_APPOINTMENT)
    public ResponseEntity<?> createUserAppointments(@CurrentUserId Long userId, @RequestBody UserScheduleAppointmentRequestDTO scheduleAppointmentRequestDTO) {
        return userAppointmentService.createUserAppointment(Math.toIntExact(userId), scheduleAppointmentRequestDTO);
    }

    @PutMapping("/reschedule")
    @LogActivity(ActionType.USER_RESCHEDULE_APPOINTMENT)
    public ResponseEntity<?> rescheduleUserAppointments(@CurrentUserId Long userId, @RequestBody UserRescheduleAppointmentRequestDTO rescheduleAppointmentRequestDTO) {
        return userAppointmentService.rescheduleUserAppointment(Math.toIntExact(userId), rescheduleAppointmentRequestDTO);
    }


    @DeleteMapping("")
    @LogActivity(ActionType.USER_CANCEL_APPOINTMENT)
    public ResponseEntity<?> cancelUserAppointment(@CurrentUserId Long userId, @RequestBody CancelAppointmentDTO cancelAppointmentDTO) {
        return userAppointmentService.cancelUserAppointment(Math.toIntExact(userId), cancelAppointmentDTO);
    }

    @PutMapping("/changeStatus/{appointmentId}")
    @CheckPermission(value = {"sales:login"})
    @LogActivity(ActionType.USER_CHANGE_APPOINTMENT_STATUS)
    public ResponseEntity<?> changeStatusUserAppointments(
            @PathVariable Integer appointmentId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof Jwt jwt)) {
            return new ResponseEntity<>(
                    new ReturnObject<>("Unauthorized", false, null),
                    HttpStatus.UNAUTHORIZED
            );
        }
        Integer userId = jwtTokenUtil.extractUserId(jwt.getTokenValue());


        return userAppointmentService.changeStatusUserAppointment(
                userId.intValue(),
                appointmentId
        );
    }

    @PutMapping("/zoomLink/{appointmentId}")
    @LogActivity(ActionType.USER_SEND_ZOOM_LINK)
    public ResponseEntity<?> sendZoomLinkByAppointmentId(
            @CurrentUserId Long userId,
            @PathVariable Integer appointmentId) {

        return userAppointmentService.sendZoomLink(
                userId.intValue(),
                appointmentId
        );
    }

    @GetMapping("/zoomData")
    @CheckPermission(value = {"sales:login"})
    @LogActivity(ActionType.USER_GET_ZOOM_DATA)
    public ResponseEntity<ReturnObject<?>> getZoomData() {

        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        Integer userId = jwtTokenUtil.extractUserId(jwt.getTokenValue());

        ReturnObject<?> result =
                userAppointmentService.getZoomDataByUser(userId);

        return ResponseEntity
                .status(result.getStatus() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }
}


