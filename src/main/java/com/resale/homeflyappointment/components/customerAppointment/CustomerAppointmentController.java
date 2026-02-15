package com.resale.homeflyappointment.components.customerAppointment;

import com.resale.homeflyappointment.components.customerAppointment.dto.HasAppointmentInfoDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.NotificationAppointmentDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.RatingDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.scheduleAppointment.ScheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.reschedule.RescheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.components.userAppointment.dto.cancelAppointment.CancelAppointmentDTO;
import com.resale.homeflyappointment.logging.LogActivity;
import com.resale.homeflyappointment.model.ActionType;
import com.resale.homeflyappointment.security.JwtTokenUtil;
import com.resale.homeflyappointment.security.customer.CurrentCustomerId;
import com.resale.homeflyappointment.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer")
public class CustomerAppointmentController {
    @Autowired
    private CustomerAppointmentService customerAppointmentService;
    @Autowired
    JwtTokenUtil jwtTokenUtil;

    @PostMapping("")
    @LogActivity(ActionType.CUSTOMER_SCHEDULE_APPOINTMENT)
    public ResponseEntity<?> scheduleAppointment(@CurrentCustomerId Long customerId, @RequestBody ScheduleAppointmentRequestDTO scheduleAppointmentRequestDTO) {
        System.out.println("id " + customerId);
        return customerAppointmentService.createAppointment(customerId, scheduleAppointmentRequestDTO);
    }




    @GetMapping("")
    @LogActivity(ActionType.GET_CUSTOMER_APPOINTMENTS)
    public ResponseEntity<?> getCustomerAppointments(@CurrentCustomerId Long customerId) {
        return customerAppointmentService.getCustomerAppointment(customerId);
    }


    @PutMapping("")
    @LogActivity(ActionType.CUSTOMER_UPDATE_APPOINTMENT)
    public ResponseEntity<?> updateCustomerAppointment(@CurrentCustomerId Long customerId, @RequestBody RescheduleAppointmentRequestDTO rescheduleAppointmentRequestDTO) {
        return customerAppointmentService.rescheduleAppointment(customerId, rescheduleAppointmentRequestDTO);
    }


    @PutMapping("/rating")
    @LogActivity(ActionType.CUSTOMER_RATE_APPOINTMENT)
    public ResponseEntity<?> appointmentRate(@CurrentCustomerId Long customerId, @RequestBody RatingDTO rateDto) {
        return customerAppointmentService.appointmentRate(customerId, rateDto);
    }


    @DeleteMapping("/{appointmentId}")
    @LogActivity(ActionType.CUSTOMER_CANCEL_APPOINTMENT)
    public ResponseEntity<?> cancelUserAppointment(
            @CurrentCustomerId Long customerId,
            @PathVariable Integer appointmentId) {

        CancelAppointmentDTO cancelAppointmentDTO = new CancelAppointmentDTO();
        cancelAppointmentDTO.setAppId(appointmentId);

        return customerAppointmentService.cancelCustomerAppointment(
                Math.toIntExact(customerId),
                cancelAppointmentDTO
        );
    }

    @GetMapping("/{appointmentId}")
    @LogActivity(ActionType.GET_CUSTOMER_APPOINTMENT)
    public ResponseEntity<NotificationAppointmentDTO> getAppointment(@PathVariable Integer appointmentId) {
        return customerAppointmentService.getAppointmentById(appointmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customerOnCall/{customerId}")
    @LogActivity(ActionType.IS_CUSTOMER_ON_CALL_INTERNAL)
    public ResponseEntity<ReturnObject<HasAppointmentInfoDTO>> isHasAppointmentByCustomerId(@PathVariable Long customerId) {
        ReturnObject returnObject = customerAppointmentService.getAppointmentByCustomerId(customerId);
        return ResponseEntity.status(HttpStatus.OK).body(returnObject);
    }


    @GetMapping("/zoom")
    @LogActivity(ActionType.GET_CUSTOMER_ZOOM_DATA)
    public ResponseEntity<ReturnObject<?>> getCustomerZoomData() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        Integer customerId = jwtTokenUtil.extractCustomerId(jwt.getTokenValue());

        ReturnObject<?> result = customerAppointmentService.getCustomerZoomData(customerId);

        return ResponseEntity
                .status(result.getStatus() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @GetMapping("/onCall")
    @LogActivity(ActionType.IS_CUSTOMER_ON_CALL)
    public ResponseEntity<ReturnObject<Boolean>> isCustomerOnCall() {

        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        Integer customerId =
                jwtTokenUtil.extractCustomerId(jwt.getTokenValue());

        ReturnObject<Boolean> result =
                customerAppointmentService.isCustomerOnCall(customerId);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/otp/generate")
    @LogActivity(ActionType.CUSTOMER_GENERATE_OTP)
    public ResponseEntity<ReturnObject<?>> generateOtp() {

        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        Integer customerId =
                jwtTokenUtil.extractCustomerId(jwt.getTokenValue());

        ReturnObject<?> result =
                customerAppointmentService.generateOtp(customerId);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping("/otp/verify")
    @LogActivity(ActionType.CUSTOMER_VERIFY_OTP)
    public ResponseEntity<ReturnObject<?>> verifyOtp(
            @RequestParam Long customerId,
            @RequestParam String otp
    ) {
        ReturnObject<Boolean> result = customerAppointmentService.verifyOtp(customerId.intValue(), otp);

        HttpStatus status = result.getStatus() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(result);
    }

}

