package com.resale.loveresaleappointment.feign;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.resale.loveresaleappointment.components.customerAppointment.dto.SingleSMSRequestDTO;
import com.resale.loveresaleappointment.components.customerAppointment.dto.UserOtpMailDTO;
import com.resale.loveresaleappointment.components.customerAppointment.dto.ZoomInfoDTO;
import com.resale.loveresaleappointment.components.userAppointment.dto.CreateZoomMeetingRequestDTO;
import com.resale.loveresaleappointment.utils.ReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(
        name = "communication-ms",
        url = "${communication.ms.url}"
)
public interface CommunicationFeignClient {

    @PostMapping("/zoom/createMeeting")
    ResponseEntity<ReturnObject<?>> createZoomMeeting(
            @RequestBody CreateZoomMeetingRequestDTO request
    );

    @PostMapping("/zoom/sendZoomLink")
    ResponseEntity<?> sendZoomLink(
            @RequestBody ZoomInfoDTO zoomInfoDTO
    );

    @GetMapping("/zoom/signature/customer/{meetingId}")
    ResponseEntity<ReturnObject<Map<String, String>>> getCustomerSignature(
            @PathVariable("meetingId") String meetingId
    );

    @GetMapping("/zoom/meeting/{meetingId}")
    ResponseEntity<ReturnObject<ObjectNode>> getZoomRuntimeData(
            @PathVariable("meetingId") String meetingId
    );

    @PutMapping("/endMeeting/{meetingId}")
    ResponseEntity<?> endZoomMeeting(@PathVariable("meetingId") String meetingId);


    @PostMapping("/sms/singleSMS")
    ResponseEntity<?> sendSingleSms(
            @RequestBody SingleSMSRequestDTO dto
    );

    @PostMapping("/mail/user/sendMail")
    ResponseEntity<?> sendMail(
            @RequestBody UserOtpMailDTO dto
    );
}


