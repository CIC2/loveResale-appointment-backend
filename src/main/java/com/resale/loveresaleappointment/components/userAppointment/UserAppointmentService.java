package com.resale.loveresaleappointment.components.userAppointment;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.resale.loveresaleappointment.components.customerAppointment.dto.ProfileResponseDTO;
import com.resale.loveresaleappointment.components.customerAppointment.dto.ZoomInfoDTO;
import com.resale.loveresaleappointment.components.teamLeadAppointment.dto.CustomerBasicInfoDTO;
import com.resale.loveresaleappointment.components.userAppointment.dto.ModelProjectDTO;
import com.resale.loveresaleappointment.components.userAppointment.dto.UserAppointmentDto;
import com.resale.loveresaleappointment.components.userAppointment.dto.cancelAppointment.CancelAppointmentDTO;
import com.resale.loveresaleappointment.components.userAppointment.dto.CreateZoomMeetingRequestDTO;
import com.resale.loveresaleappointment.components.userAppointment.dto.rescheduleAppointment.UserRescheduleAppointmentRequestDTO;
import com.resale.loveresaleappointment.components.userAppointment.dto.scheduleAppointment.UserScheduleAppointmentRequestDTO;
import com.resale.loveresaleappointment.feign.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resale.loveresaleappointment.model.Appointment;
import com.resale.loveresaleappointment.repos.AppointmentRepository;
import com.resale.loveresaleappointment.utils.CustomerValidationService;
import com.resale.loveresaleappointment.utils.MessageUtil;
import com.resale.loveresaleappointment.utils.PaginatedResponseDTO;
import com.resale.loveresaleappointment.utils.ReturnObject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class UserAppointmentService {

    private final CustomerFeignClient customerFeignClient;
    private final UserFeignClient userFeignClient;
    private final AppointmentRepository appointmentRepository;
    private final MessageUtil messageUtil;
    private final CustomerValidationService customerValidationService;
    private final CommunicationFeignClient communicationFeignClient;
    private final ObjectMapper objectMapper;
    private final InventoryFeignClient inventoryFeignClient;

    public UserAppointmentService(CustomerFeignClient customerFeignClient,
                                  UserFeignClient userFeignClient,
                                  AppointmentRepository appointmentRepository,
                                  MessageUtil messageUtil,
                                  CustomerValidationService customerValidationService,
                                  CommunicationFeignClient communicationFeignClient,
                                  ObjectMapper objectMapper,
                                  InventoryFeignClient inventoryFeignClient) {
        this.customerFeignClient = customerFeignClient;
        this.userFeignClient = userFeignClient;
        this.appointmentRepository = appointmentRepository;
        this.messageUtil = messageUtil;
        this.customerValidationService = customerValidationService;
        this.communicationFeignClient = communicationFeignClient;
        this.objectMapper = objectMapper;
        this.inventoryFeignClient = inventoryFeignClient;
    }


    public ResponseEntity<?> createUserAppointment(Integer userId, UserScheduleAppointmentRequestDTO scheduleAppointmentRequestDTO) {

        System.out.println("---- [Create Appointment] START ----");
        System.out.println("Incoming userId: " + userId);
        System.out.println("Incoming mobile: " + scheduleAppointmentRequestDTO.getMobile());

        Long customerId = null;
        ProfileResponseDTO customerProfile = null;

        // ---------------------------------------------------------
        // 1️⃣ Fetch Customer Details by Mobile
        // ---------------------------------------------------------
        try {
            ResponseEntity<ReturnObject<ProfileResponseDTO>> customerResponse =
                    customerFeignClient.getCustomerProfileByMobileAndCountryCode(
                            Math.toIntExact(scheduleAppointmentRequestDTO.getMobile()),
                            scheduleAppointmentRequestDTO.getCountryCode()
                    );

            if (customerResponse.getStatusCode().is2xxSuccessful() &&
                    customerResponse.getBody() != null &&
                    customerResponse.getBody().getData() != null) {

                customerProfile = customerResponse.getBody().getData();
                customerId = customerProfile.getCustomerId();

                System.out.println("Customer ID: " + customerId);

            } else {
                System.out.println("⚠️ Failed to fetch customer details. Continuing without customer data.");
            }

        } catch (Exception ex) {
            System.out.println("❌ Error calling Customer Microservice: " + ex.getMessage());
        }

        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ReturnObject<>("Customer not found", false, null)
            );
        }

        Optional<List<Appointment>> customerAppointmentsOptional =
                appointmentRepository.findAllByCustomerIdAndStatus(customerId, "OPEN");

        if (customerAppointmentsOptional.isPresent()) {
            List<Appointment> appointmentList = customerAppointmentsOptional.get();
            if (!appointmentList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ReturnObject<>("Customer already has appointment(s)", false, null)
                );
            }
        }

        // ---------------------------------------------------------
        // 2️⃣ Create Appointment Locally
        // ---------------------------------------------------------
        Appointment newApp = new Appointment();
        newApp.setUserId(userId);
        newApp.setStatus("OPEN");
        newApp.setCustomerId(customerId);

        try {
            newApp.setAppointmentDate(Timestamp.valueOf(scheduleAppointmentRequestDTO.getSchedule()));
        } catch (Exception ex) {
            System.out.println("⚠️ Invalid date format, schedule not set.");
        }

        newApp.setType("SALES SCHEDULE");

        // ---------------------------------------------------------
        // 3️⃣ Save Appointment Locally
        // ---------------------------------------------------------
        try {
            appointmentRepository.save(newApp);
            System.out.println("Local appointment saved ID = " + newApp.getId());
        } catch (Exception ex) {
            System.out.println("❌ Failed to save appointment locally: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Failed locally: " + ex.getMessage(), false, null)
            );
        }

        // ---------------------------------------------------------
        // 4️⃣ Final Response
        // ---------------------------------------------------------
        System.out.println("---- [Create Appointment] END SUCCESS ----");

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ReturnObject<>(
                        messageUtil.getMessage("appointment.created.successfully"),
                        true,
                        newApp
                )
        );
    }


    public ResponseEntity<?> rescheduleUserAppointment(Integer userId, UserRescheduleAppointmentRequestDTO req) {

        System.out.println("---- [Reschedule Appointment] START ----");
        System.out.println("Incoming userId: " + userId);
        System.out.println("Incoming appId: " + req.getAppId());
        System.out.println("Incoming new schedule: " + req.getSchedule());

        // ---------------------------------------------------------
        // 1️⃣ Fetch Existing Appointment
        // ---------------------------------------------------------
        Optional<Appointment> optAppointment =
                appointmentRepository.getAppointmentByUserIdAndIdAndStatus(
                        userId,
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


    public ResponseEntity<ReturnObject<PaginatedResponseDTO<UserAppointmentDto>>> getUserAppointment(
            Long userId,
            String name,
            Long mobile,
            String fromDate,
            String toDate,
            Pageable pageable
    ) {
        try {
            List<Appointment> userAppointments =
                    appointmentRepository.findAllByUserId(userId.intValue());

            userAppointments = userAppointments.stream()
                    .filter(app -> "OPEN".equalsIgnoreCase(app.getStatus()))
                    .collect(Collectors.toList());

            String mobileStringValue = mobile != null ? mobile.toString() : null;

            if (userAppointments.isEmpty()) {
                PaginatedResponseDTO<UserAppointmentDto> emptyPage = new PaginatedResponseDTO<>(
                        Collections.emptyList(), 0, pageable.getPageSize(), 0, 0, true
                );
                return ResponseEntity.ok(new ReturnObject<>("No appointments found", false, emptyPage));
            }

            List<Long> customerIds = userAppointments.stream()
                    .map(Appointment::getCustomerId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            System.out.println("Unique Customer IDs: " + customerIds);

            Map<Long, ProfileResponseDTO> customerMap = new HashMap<>();
            if (!customerIds.isEmpty()) {
                ResponseEntity<ReturnObject<List<ProfileResponseDTO>>> customerResponse =
                        customerFeignClient.getCustomersByIds(customerIds, name, mobileStringValue);

                if (customerResponse != null &&
                        customerResponse.getBody() != null &&
                        customerResponse.getBody().getStatus()) {

                    customerMap = customerResponse.getBody().getData().stream()
                            .collect(Collectors.toMap(ProfileResponseDTO::getCustomerId, c -> c));
                }
            }

            final Map<Long, ProfileResponseDTO> finalCustomerMap = customerMap;

            LocalDateTime from = fromDate != null ? LocalDateTime.parse(fromDate) : null;
            LocalDateTime to = toDate != null ? LocalDateTime.parse(toDate) : null;

            List<UserAppointmentDto> appointmentDtos = userAppointments.stream()
                    .filter(app -> finalCustomerMap.containsKey(app.getCustomerId()))
                    .filter(app -> {
                        LocalDateTime appDate = app.getAppointmentDate().toLocalDateTime();
                        if (from != null && appDate.isBefore(from)) return false;
                        if (to != null && appDate.isAfter(to)) return false;
                        return true;
                    })
                    .map(app -> {
                        ProfileResponseDTO customer = finalCustomerMap.get(app.getCustomerId());
                        return new UserAppointmentDto(
                                app.getId(),
                                app.getCustomerId(),
                                app.getStatus(),
                                customer != null ? customer.getFullName() : "Unknown",
                                customer != null ? customer.getCountryCode() : null,
                                customer != null ? customer.getMobile() : "N/A",
                                app.getAppointmentDate()
                        );
                    })
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), appointmentDtos.size());
            List<UserAppointmentDto> pagedList = start >= end ? Collections.emptyList() : appointmentDtos.subList(start, end);

            PaginatedResponseDTO<UserAppointmentDto> paginatedResponse = new PaginatedResponseDTO<>(
                    pagedList,
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    appointmentDtos.size(),
                    (int) Math.ceil((double) appointmentDtos.size() / pageable.getPageSize()),
                    end >= appointmentDtos.size()
            );

            return ResponseEntity.ok(
                    new ReturnObject<>(
                            messageUtil.getMessage("loaded.successfully"),
                            true,
                            paginatedResponse
                    )
            );

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReturnObject<>(
                            "Unexpected error: " + ex.getMessage(),
                            false,
                            null
                    ));
        }
    }


    public ResponseEntity<ReturnObject<?>> cancelUserAppointment(Integer userId, CancelAppointmentDTO cancelAppointmentDTO) {

        Optional<Appointment> optAppointment =
                appointmentRepository.getAppointmentByUserIdAndIdAndStatus(
                        userId,
                        Integer.valueOf(cancelAppointmentDTO.getAppId()),
                        "OPEN"
                );

        if (optAppointment.isEmpty()) {
            return ResponseEntity.ok(
                    new ReturnObject<>(
                            "No appointments found for this user with this Id",
                            false,
                            Collections.emptyList()
                    )
            );
        }

        Appointment appointment = optAppointment.get();
        appointment.setStatus("CANCELED");
        appointmentRepository.save(appointment);

        return ResponseEntity.ok(
                new ReturnObject<>(
                        "Appointment canceled successfully",
                        true,
                        Collections.emptyList()
                )
        );
    }


    @Transactional
    public ResponseEntity<ReturnObject<?>> changeStatusUserAppointment(
            Integer userId,
            Integer appointmentId
    ) {

        Optional<Appointment> optionalAppointment =
                appointmentRepository.getAppointmentByUserIdAndId(userId, appointmentId);

        if (optionalAppointment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ReturnObject<>(
                            "Appointment not found for this user",
                            false,
                            null
                    ));
        }

        Appointment appointment = optionalAppointment.get();
        String oldStatus = appointment.getStatus();

        if (!"OPEN".equals(oldStatus) || appointment.getZoomMeetingId() != null) {
            return ResponseEntity.badRequest()
                    .body(new ReturnObject<>(
                            "Appointment cannot be changed: status=" + oldStatus
                                    + ", zoomMeetingId=" + appointment.getZoomMeetingId(),
                            false,
                            null
                    ));
        }

        boolean alreadyOnCall =
                appointmentRepository.existsByUserIdAndStatusAndIdNot(
                        userId,
                        "ON_CALL",
                        appointmentId
                );

        if (alreadyOnCall) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ReturnObject<>(
                            "Salesman already has an active on call appointment",
                            false,
                            null
                    ));
        }

        boolean salesmanOnCall =
                appointmentRepository.existsByUserIdAndStatus(appointment.getUserId(), "ON_CALL");

        if (salesmanOnCall) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ReturnObject<>("Salesman is already on another call", false, null));
        }

        CreateZoomMeetingRequestDTO req = new CreateZoomMeetingRequestDTO();
        req.setTopic("Sales Appointment");
        req.setStart_time(appointment.getAppointmentDate().toInstant().toString());
        req.setDuration(120);
        req.setZoomHostId("me");

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
            appointment.setZoomMeetingId(String.valueOf(zoomData.get("meetingId")));
            appointment.setZoomUrl((String) zoomData.get("joinUrl"));
            appointment.setZoomStartUrl((String) zoomData.get("startUrl"));
        }

        appointment.setStatus("ON_CALL");
        appointment.setStartTime(Timestamp.valueOf(LocalDateTime.now()));
        appointmentRepository.save(appointment);

        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("appointmentId", appointment.getId());
        responseData.put("status", "ON_CALL");

        return ResponseEntity.ok(
                new ReturnObject<>(
                        "Status changed from OPEN to ON_CALL",
                        true,
                        responseData
                )
        );
    }


    public ResponseEntity<ReturnObject<?>> sendZoomLink(Integer userId, Integer appointmentId) {

        Optional<Appointment> optionalAppointment =
                appointmentRepository.getAppointmentByUserIdAndId(userId, appointmentId);

        if (optionalAppointment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ReturnObject<>("Appointment not found for this user", false, null));
        }

        Appointment appointment = optionalAppointment.get();
        List<Long> customerIds = new ArrayList<>();
        customerIds.add(appointment.getCustomerId());
        ReturnObject returnObject = new ReturnObject();

        ResponseEntity<ReturnObject<List<ProfileResponseDTO>>> customerResponse =
                customerFeignClient.getCustomersByIds(customerIds, null, null);

        ZoomInfoDTO zoomInfoDTO = null;
        if (customerResponse != null &&
                customerResponse.getBody() != null &&
                customerResponse.getBody().getStatus()) {

            ProfileResponseDTO profileResponseDTO = customerResponse.getBody().getData().get(0);
            zoomInfoDTO = new ZoomInfoDTO();
            zoomInfoDTO.setCustomerId(appointment.getCustomerId());
            zoomInfoDTO.setUserId(userId);
            zoomInfoDTO.setCustomerMobile(profileResponseDTO.getMobile());
            zoomInfoDTO.setCustomerName(profileResponseDTO.getFullName());
            zoomInfoDTO.setCustomerMail("");
            zoomInfoDTO.setCustomerFirebaseToken(profileResponseDTO.getFcmToken());
            zoomInfoDTO.setZoomUrl(appointment.getZoomUrl());
            zoomInfoDTO.setAppointmentId(appointmentId);

            try {
                communicationFeignClient.sendZoomLink(zoomInfoDTO);
                returnObject.setMessage("Sent Successfully To Mobile : " + zoomInfoDTO.getCustomerMobile() + " and Mail : " + zoomInfoDTO.getCustomerMail());
                returnObject.setData(zoomInfoDTO);
                returnObject.setStatus(true);
                return ResponseEntity.ok(returnObject);

            } catch (Exception exception) {
                returnObject.setMessage("Failed to Send :");
                returnObject.setData(null);
                returnObject.setStatus(false);
                System.out.println("Failed To Send");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(returnObject);
            }
        }

        returnObject.setMessage("Failed to Send :");
        returnObject.setData(null);
        returnObject.setStatus(false);
        System.out.println("Failed To Send");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(returnObject);
    }


    public ReturnObject<ObjectNode> getZoomDataByUser(Integer userId) {

        Optional<Appointment> appointmentOptional =
                appointmentRepository.findByUserIdAndStatusIgnoreCase(userId, "ON_CALL");

        if (appointmentOptional.isEmpty()) {
            return new ReturnObject<>(
                    "No active on call appointment found for this user",
                    false,
                    null
            );
        }

        Appointment appointment = appointmentOptional.get();

        if (appointment.getZoomMeetingId() == null) {
            return new ReturnObject<>(
                    "Zoom meeting not created yet",
                    false,
                    null
            );
        }

        ResponseEntity<ReturnObject<ObjectNode>> zoomResponse =
                communicationFeignClient.getZoomRuntimeData(appointment.getZoomMeetingId());

        if (!zoomResponse.getStatusCode().is2xxSuccessful()
                || zoomResponse.getBody() == null
                || !zoomResponse.getBody().getStatus()) {

            return new ReturnObject<>(
                    "Failed to retrieve Zoom data",
                    false,
                    null
            );
        }

        ObjectNode data = zoomResponse.getBody().getData();
        data.put("appointmentId", appointment.getId());
        data.put("customerId", appointment.getCustomerId());

        String customerName = "N/A";
        try {
            ResponseEntity<ReturnObject<CustomerBasicInfoDTO>> customerResponse =
                    customerFeignClient.getCustomerBasicInfo(appointment.getCustomerId());

            if (customerResponse.getStatusCode().is2xxSuccessful()
                    && customerResponse.getBody() != null
                    && customerResponse.getBody().getStatus()
                    && customerResponse.getBody().getData() != null) {

                customerName = customerResponse.getBody().getData().getFullName();
            }
        } catch (Exception ex) {
            log.warn("Failed to retrieve customer name for customerId={}", appointment.getCustomerId(), ex);
        }

        data.put("customerName", customerName);
        data.put("startUrl", appointment.getZoomStartUrl());
        data.remove("joinUrl");

        try {
            ResponseEntity<ReturnObject<ModelProjectDTO>> modelResponse =
                    inventoryFeignClient.getModelProject(appointment.getModelId());

            data.put("modelId", appointment.getModelId());

            if (modelResponse.getStatusCode().is2xxSuccessful()
                    && modelResponse.getBody() != null
                    && modelResponse.getBody().getStatus()
                    && modelResponse.getBody().getData() != null) {

                ModelProjectDTO modelProjectDTO = modelResponse.getBody().getData();
                data.put("modelName", modelProjectDTO.getModelName());
                data.put("modelCode", modelProjectDTO.getModelCode());
                data.put("projectId", modelProjectDTO.getProjectId());
                data.put("projectName", modelProjectDTO.getProjectName());
            } else {
                data.put("modelName", "N/A");
                data.put("modelCode", "N/A");
                data.putNull("projectId");
                data.put("projectName", "N/A");
            }
        } catch (Exception ex) {
            log.warn("Failed to retrieve model/project info for modelId={}", appointment.getModelId(), ex);
            data.put("modelName", "N/A");
            data.put("modelCode", "N/A");
            data.put("projectId", "N/A");
            data.put("projectName", "N/A");
            data.put("modelId", appointment.getModelId());
        }

        return new ReturnObject<>(
                "Host Zoom data retrieved",
                true,
                data
        );
    }
}