package com.resale.loveresaleappointment.components.queueAppointment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.resale.loveresaleappointment.components.internal.dto.CloseAppointmentDTO;
import com.resale.loveresaleappointment.components.queueAppointment.dto.AppointmentInfoDTO;
import com.resale.loveresaleappointment.components.queueAppointment.dto.QueueAppointmentRequestDTO;
import com.resale.loveresaleappointment.components.userAppointment.dto.CreateZoomMeetingRequestDTO;
import com.resale.loveresaleappointment.feign.CommunicationFeignClient;
import com.resale.loveresaleappointment.feign.QueueMsFeignClient;
import com.resale.loveresaleappointment.model.Appointment;
import com.resale.loveresaleappointment.repos.AppointmentRepository;
import com.resale.loveresaleappointment.utils.ReturnObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.sql.Timestamp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;


@Service
@Slf4j
public class QueueAppointmentService {

    @Autowired
    AppointmentRepository appointmentRepository;
    @Autowired
    CommunicationFeignClient communicationFeignClient;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    QueueMsFeignClient queueMsFeignClient;

    public ResponseEntity<?> createAppointmentFromQueue(QueueAppointmentRequestDTO dto) {
        try {
            boolean salesmanOnCall =
                    appointmentRepository.existsByUserIdAndStatus(dto.getSalesmanId(), "ON_CALL");

            if (salesmanOnCall) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ReturnObject<>(
                                "Salesman is already on an active call", false, null));
            }

            Appointment app = new Appointment();
            app.setUserId(dto.getSalesmanId());
            app.setCustomerId(dto.getCustomerId());
            app.setStatus("ON_CALL");
            app.setType("QUEUE_CALL");
            app.setStartTime(dto.getCallStartTime());
            app.setAppointmentDate(dto.getAppointmentDate());
            app.setModelId(dto.getModelId());


            String zoomHostId = "me";

            CreateZoomMeetingRequestDTO req = new CreateZoomMeetingRequestDTO();
            req.setTopic("Queue Call");
            req.setStart_time(dto.getAppointmentDate().toInstant().toString());
            req.setDuration(120);
            req.setZoomHostId(zoomHostId);

            ResponseEntity<ReturnObject<?>> zoomResponse =
                    communicationFeignClient.createZoomMeeting(req);

            if (!zoomResponse.getStatusCode().is2xxSuccessful()
                    || zoomResponse.getBody() == null
                    || !zoomResponse.getBody().getStatus()) {

                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ReturnObject<>(
                                "Failed to create Zoom meeting",
                                false,
                                null
                        ));
            }

            ReturnObject<?> body = zoomResponse.getBody();

            if (body.getData() instanceof Map<?, ?> zoomData) {
                app.setZoomMeetingId(String.valueOf(zoomData.get("meetingId")));
                app.setZoomUrl((String) zoomData.get("joinUrl"));
                app.setZoomStartUrl((String) zoomData.get("startUrl"));
            }

            appointmentRepository.save(app);


            ObjectNode responseData = objectMapper.createObjectNode();
            responseData.put("appointmentId", app.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ReturnObject<>(
                            "Appointment created from queue",
                            true,
                            responseData
                    ));

        } catch (Exception ex) {
            log.error("Failed to create appointment from queue", ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReturnObject<>(
                            "Failed to create appointment from queue",
                            false,
                            null
                    ));
        }
    }


    public ResponseEntity<?> endCallFromQueue(Integer salesmanId) {
        try {
            Optional<Appointment> optionalApp =
                    appointmentRepository.findByUserIdAndStatusIgnoreCase(salesmanId,"ON_CALL");

            if (optionalApp.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ReturnObject<>(
                                "Appointment not found",
                                false,
                                null
                        ));
            }

            Appointment appointment = optionalApp.get();
            String oldStatus = appointment.getStatus();

            appointment.setStatus("CLOSED");
            appointment.setEndTime(new Timestamp(System.currentTimeMillis()));
            appointmentRepository.save(appointment);

            // End Zoom meeting if exists
            if ("ON_CALL".equals(oldStatus)
                    && appointment.getZoomMeetingId() != null
                    && !appointment.getZoomMeetingId().isBlank()) {

                try {
                    communicationFeignClient.endZoomMeeting(
                            appointment.getZoomMeetingId()
                    );
                } catch (Exception ex) {
                    log.error(
                            "Failed to end Zoom meeting {}",
                            appointment.getZoomMeetingId(),
                            ex
                    );
                }
            }
            CloseAppointmentDTO closeAppointmentDTO = new CloseAppointmentDTO(String.valueOf(appointment.getId()),
                    "CLOSED");
            return ResponseEntity.ok(
                    new ReturnObject<>(
                            "Appointment closed successfully",
                            true,
                            closeAppointmentDTO
                    )
            );

        } catch (Exception ex) {
            log.error("Failed to end appointment for salesmanId {}", salesmanId , ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReturnObject<>(
                            "Failed to end appointment",
                            false,
                            null
                    ));
        }
    }

    public ReturnObject<AppointmentInfoDTO> getAppointmentInfoById(Integer appointmentId) {
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(appointmentId);

        if (optionalAppointment.isEmpty()) {
            return new ReturnObject<>("Appointment not found", false, null);
        }

        Appointment appointment = optionalAppointment.get();
        AppointmentInfoDTO dto = new AppointmentInfoDTO(
                appointment.getId(),
                appointment.getUserId(),
                appointment.getCustomerId()
        );

        return new ReturnObject<>("Appointment fetched successfully", true, dto);
    }

    public ReturnObject<String> sendAppointmentToQueue(AppointmentInfoDTO dto, Integer userId) {
        try {

            Optional<Appointment> optionalAppointment = appointmentRepository.findById(dto.getAppointmentId());

            if (optionalAppointment.isEmpty()) {
                return new ReturnObject<>("Appointment not found", false, null);
            }

            Appointment app = optionalAppointment.get();

            if (!app.getUserId().equals(userId)) {
                return new ReturnObject<>("User does not belong to this appointment", false, null);
            }

            boolean salesmanOnCall = appointmentRepository.existsByUserIdAndStatus(userId, "ON_CALL");

            if (salesmanOnCall && !"ON_CALL".equals(app.getStatus())) {
                return new ReturnObject<>("Salesman is already on another active call", false, null);
            }

            //  Update status to ON_CALL
            app.setStatus("ON_CALL");
            appointmentRepository.save(app);

            // Create Zoom meeting
            CreateZoomMeetingRequestDTO zoomReq = new CreateZoomMeetingRequestDTO();
            zoomReq.setTopic("Queue Call");
            zoomReq.setStart_time(app.getAppointmentDate().toInstant().toString());
            zoomReq.setDuration(120);
            zoomReq.setZoomHostId("me"); //will change to host id later

            ResponseEntity<ReturnObject<?>> zoomResponse = communicationFeignClient.createZoomMeeting(zoomReq);

            if (!zoomResponse.getStatusCode().is2xxSuccessful() ||
                    zoomResponse.getBody() == null ||
                    !zoomResponse.getBody().getStatus()) {
                return new ReturnObject<>("Failed to create Zoom meeting", false, null);
            }

            // Save Zoom info in appointment
            ReturnObject<?> zoomBody = zoomResponse.getBody();
            if (zoomBody.getData() instanceof Map<?, ?> zoomData) {
                app.setZoomMeetingId(String.valueOf(zoomData.get("meetingId")));
                app.setZoomUrl((String) zoomData.get("joinUrl"));
                app.setZoomStartUrl((String) zoomData.get("startUrl"));
                app.setStartTime(Timestamp.valueOf(LocalDateTime.now()));
            }
            appointmentRepository.save(app);

            // Notify Queue MS
            AppointmentInfoDTO queueCallDTO = new AppointmentInfoDTO(
                    app.getId(),
                    app.getUserId(),
                    app.getCustomerId()
            );
            queueMsFeignClient.notifyNewCall(queueCallDTO);

            return new ReturnObject<>("Joined Queue successfully", true, null);

        } catch (Exception ex) {
            log.error("Failed to send appointment to queue", ex);
            return new ReturnObject<>("Failed to join Queue: " + ex.getMessage(), false, null);
        }
    }
}


