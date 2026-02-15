package com.resale.homeflyappointment.components.customerAppointment;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.resale.homeflyappointment.components.customerAppointment.dto.*;
import com.resale.homeflyappointment.components.customerAppointment.dto.availableSlots.AppointmentSlotsResponseDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.reschedule.RescheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.reschedule.RescheduleAppointmentSapResponse;
import com.resale.homeflyappointment.components.customerAppointment.dto.scheduleAppointment.ScheduleAppointmentRequestDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.scheduleAppointment.ScheduleAppointmentSapResponse;
import com.resale.homeflyappointment.components.teamLeadAppointment.dto.CustomerBasicInfoDTO;
import com.resale.homeflyappointment.components.userAppointment.dto.UserProfileResponseDTO;
import com.resale.homeflyappointment.components.userAppointment.dto.cancelAppointment.C4cCancelAppointmentDto;
import com.resale.homeflyappointment.components.userAppointment.dto.cancelAppointment.CancelAppointmentDTO;
import com.resale.homeflyappointment.feign.*;
import com.resale.homeflyappointment.model.Appointment;
import com.resale.homeflyappointment.model.AppointmentOtp;
import com.resale.homeflyappointment.repos.AppointmentOtpRepository;
import com.resale.homeflyappointment.repos.AppointmentRepository;
import com.resale.homeflyappointment.utils.CustomerValidationService;
import com.resale.homeflyappointment.utils.MessageUtil;
import com.resale.homeflyappointment.utils.ReturnObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class CustomerAppointmentService {


    private final SAPFeignClientC4c sapFeignClientC4c;
    private final CustomerFeignClient customerFeignClient;
    private final AppointmentRepository appointmentRepository;
    private final MessageUtil messageUtil;
    private final UserFeignClient userFeignClient;
    private final CustomerValidationService customerValidationService;
    private final CommunicationFeignClient communicationFeignClient;
    private final AppointmentOtpRepository appointmentOtpRepository;
    private final CustomerMSFeignClient customerMSFeignClient;

    public CustomerAppointmentService(SAPFeignClientC4c sapFeignClientC4c,
                                      CustomerFeignClient customerFeignClient,
                                      AppointmentRepository appointmentRepository,
                                      MessageUtil messageUtil,
                                      UserFeignClient userFeignClient,
                                      CustomerValidationService customerValidationService, CommunicationFeignClient communicationFeignClient, AppointmentOtpRepository appointmentOtpRepository,
                                      CustomerMSFeignClient customerMSFeignClient) {
        this.sapFeignClientC4c = sapFeignClientC4c;
        this.customerFeignClient = customerFeignClient;
        this.appointmentRepository = appointmentRepository;
        this.messageUtil = messageUtil;
        this.userFeignClient = userFeignClient;
        this.customerValidationService = customerValidationService;
        this.communicationFeignClient = communicationFeignClient;
        this.appointmentOtpRepository = appointmentOtpRepository;
        this.customerMSFeignClient = customerMSFeignClient;
    }


    public ResponseEntity<?> createAppointment(Long customerId, ScheduleAppointmentRequestDTO scheduleAppointmentRequestDTO) {
        ResponseEntity<ReturnObject<CustomerValidationResultDTO>> validationResponse =
                customerFeignClient.validateCustomer(customerId);

        // 2. run validation helper
        ReturnObject<CustomerValidationResultDTO> validationError =
                customerValidationService.extractCustomerValidationError(validationResponse);

        // 3. if error exists → return it
        if (validationError != null) {
            return ResponseEntity.badRequest().body(validationError);
        }


        if (scheduleAppointmentRequestDTO.getSchedule().isEmpty()) {
            ReturnObject error = new ReturnObject();
            error.setStatus(false);
            error.setMessage("Need to Add Date for Appointment");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        try {
            System.out.println("customerId: " + customerId);

            // 1️⃣ Fetch existing OPEN appointments
            Optional<List<Appointment>> customerAppointmentsOptional =
                    appointmentRepository.findAllByCustomerIdAndStatus(customerId, "OPEN");

            if (customerAppointmentsOptional.isPresent() && !customerAppointmentsOptional.get().isEmpty()) {
                return ResponseEntity.ok(
                        new ReturnObject<>(
                                "Customer already has appointment(s)",
                                false,
                                customerAppointmentsOptional.get()
                        )
                );
            }

            System.out.println("---- [Create Appointment] START ----" + customerId);

            // 2️⃣ Fetch customer profile
            ResponseEntity<ReturnObject<ProfileResponseDTO>> customerResponse =
                    customerFeignClient.getCustomerProfile(customerId);

            if (!customerResponse.getStatusCode().is2xxSuccessful()
                    || customerResponse.getBody() == null
                    || customerResponse.getBody().getData() == null) {
                throw new RuntimeException("Failed to fetch customer info from Customer Service");
            }

            ProfileResponseDTO customerProfile = customerResponse.getBody().getData();
            scheduleAppointmentRequestDTO.setSapPartnerId(customerProfile.getSapPartnerId());

            if (scheduleAppointmentRequestDTO.getSapPartnerId() == null
                    || scheduleAppointmentRequestDTO.getSapPartnerId().isEmpty()) {

                ReturnObject error = new ReturnObject();
                error.setStatus(false);
                error.setMessage("No Assigned Business Partner");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // 3️⃣ Assign Salesman using Round Robin API
            System.out.println("Calling Round Robin Salesman Assignment API...");

            ResponseEntity<ReturnObject<?>> rrResponse = userFeignClient.getAssignSalesmanRoundRobin(scheduleAppointmentRequestDTO.getProjectId());

            if (!rrResponse.getStatusCode().is2xxSuccessful()
                    || rrResponse.getBody() == null
                    || rrResponse.getBody().getData() == null) {
                throw new RuntimeException("Failed to get Salesman from Round Robin API");
            }

            Object data = rrResponse.getBody().getData();

            Map<String, Object> salesmanMap = (Map<String, Object>) data;

            Long assignedSalesmanId = Long.valueOf(salesmanMap.get("id").toString());
            System.out.println("Assigned Salesman ID: " + assignedSalesmanId);

            // 4️⃣ Create Appointment Entity
            Appointment newApp = new Appointment();
            newApp.setCustomerId(customerId);
            newApp.setModelId(scheduleAppointmentRequestDTO.getModelId());

            // Convert schedule datetime
            String rawDate = scheduleAppointmentRequestDTO.getSchedule();
            rawDate = rawDate.replaceAll("(\\+|\\-)(\\d{2})(\\d{2})$", "$1$2:$3");
            OffsetDateTime odt = OffsetDateTime.parse(rawDate);
            Timestamp timestamp = Timestamp.from(odt.toInstant());
            newApp.setAppointmentDate(timestamp);

            newApp.setType("CUSTOMER SCHEDULE");
            newApp.setStatus("OPEN");
            newApp.setUserId(assignedSalesmanId.intValue());  // 👈 Assigned salesman

            // 5️⃣ Call SAP C4C to create appointment
            try {
                ResponseEntity<ScheduleAppointmentSapResponse> response =
                        sapFeignClientC4c.customerScheduleAppointment(scheduleAppointmentRequestDTO);

                ScheduleAppointmentSapResponse sapResponse = response.getBody();

                if (response.getStatusCode().is2xxSuccessful() && sapResponse != null) {
                    newApp.setC4CId(sapResponse.getAppointmentId());
                    System.out.println("Appointment created in SAP ID: " + sapResponse.getAppointmentId());
                } else {
                    newApp.setC4CId("FAIL");
                    System.out.println("⚠️ SAP appointment creation failed.");
                }
            } catch (Exception e) {
                System.out.println("-Error in calling SAP (C4C)");
                newApp.setC4CId("SAP_EXCEPTION");
                System.out.println("⚠️ SAP appointment creation failed.");
            }

            // 6️⃣ Save to local DB
            appointmentRepository.save(newApp);

            // 7️⃣ Return object
            ReturnObject responseObj = new ReturnObject();
            responseObj.setStatus(true);
            responseObj.setData(newApp);
            responseObj.setMessage(messageUtil.getMessage("appointment.created.successfully"));

            return ResponseEntity.status(HttpStatus.CREATED).body(responseObj);

        } catch (Exception e) {
            System.out.println("❌ Exception during appointment creation: " + e.getMessage());
            e.printStackTrace();

            ReturnObject error = new ReturnObject();
            error.setStatus(false);
            error.setMessage("Failed to create appointment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    public ResponseEntity<ReturnObject<?>> findAvailableAppointments(Long customerId) {
        try {
            System.out.println("customerId: " + customerId);

            // Fetch existing appointments
            Optional<List<Appointment>> customerAppointmentsOptional =
                    appointmentRepository.findAllByCustomerIdAndStatus(customerId, "OPEN");

            if (customerAppointmentsOptional.isPresent()) {
                List<Appointment> customerAppointments = customerAppointmentsOptional.get();

                // If customer already has appointments, return them
                if (!customerAppointments.isEmpty()) {
                    return ResponseEntity.ok(
                            new ReturnObject<>(
                                    "Customer already has appointment(s)",
                                    false,
                                    customerAppointments
                            )
                    );
                }
            }

            // Call SAP to get available slots
            ResponseEntity<AppointmentSlotsResponseDTO> response = sapFeignClientC4c.getAvailableSlots();

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ReturnObject<>(
                                "Failed to fetch available slots from SAP",
                                false,
                                null
                        ));
            }

            AppointmentSlotsResponseDTO sapResponse = response.getBody();

            return ResponseEntity.ok(
                    new ReturnObject<>(
                            messageUtil.getMessage("loaded.successfully"),
                            true,
                            sapResponse
                    )
            );

        } catch (Exception ex) {
            ex.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReturnObject<>(
                            "Unexpected error while fetching appointments: " + ex.getMessage(),
                            false,
                            null
                    ));
        }
    }

    public ResponseEntity<ReturnObject<?>> getCustomerAppointment(Long customerId) {
        try {
            List<String> statuses = Arrays.asList("OPEN", "ON_CALL");
            Optional<List<Appointment>> customerAppointmentsOptional =
                    appointmentRepository.findAllByCustomerIdAndStatusIn(customerId.intValue(),statuses);

            // If no list or empty list
            if (customerAppointmentsOptional.isPresent()) {
                List<Appointment> appointmentList = customerAppointmentsOptional.get();
                if (appointmentList.isEmpty()) {
                    return ResponseEntity.ok(
                            new ReturnObject<>(
                                    "No appointments found for this customer",
                                    true,
                                    Collections.emptyList()
                            )
                    );
                }

                List<Appointment> customerAppointments = customerAppointmentsOptional.get();

                // Build list of AppointmentDto
                List<AppointmentDto> dtoList = new ArrayList<>();

                // For each appointment → fetch user name → build dto
                for (Appointment appointment : customerAppointments) {

                    ResponseEntity<ReturnObject<UserProfileResponseDTO>> userResponse =
                            userFeignClient.getUserProfile((long) appointment.getUserId());

                    String fullName = userResponse.getBody().getData().getFullName();

                    AppointmentDto dto = new AppointmentDto(
                            appointment.getId(),
                            fullName,
                            appointment.getAppointmentDate(),
                            appointment.getStatus()
                    );

                    dtoList.add(dto);
                }

                return ResponseEntity.ok(
                        new ReturnObject<>(
                                messageUtil.getMessage("loaded.successfully"),
                                true,
                                dtoList
                        )
                );
            }
            return ResponseEntity.ok(
                    new ReturnObject<>(
                            messageUtil.getMessage("No appointments found for this customer"),
                            true,
                            List.of()
                    )
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReturnObject<>(
                            "Unexpected error while fetching appointments: " + ex.getMessage(),
                            false,
                            null
                    ));
        }
    }

    public ResponseEntity<?> rescheduleAppointment(Long customerId, RescheduleAppointmentRequestDTO
            rescheduleAppointmentRequestDTO) {
        if (rescheduleAppointmentRequestDTO.getSchedule() == null) {
            ReturnObject error = new ReturnObject();
            error.setStatus(false);
            error.setMessage("Need to Add Date for Appointment");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        try {
            System.out.println("customerId: " + customerId);

            System.out.println("---- [Update Appointment] START ----" + customerId);

            // Call Customer Microservice securely
            ResponseEntity<ReturnObject<ProfileResponseDTO>> customerResponse =
                    customerFeignClient.getCustomerProfile(customerId);

            if (!customerResponse.getStatusCode().is2xxSuccessful() || customerResponse.getBody().getData() == null) {
                throw new RuntimeException("Failed to fetch customer info from Customer Service");
            }

            ProfileResponseDTO customerProfile = customerResponse.getBody().getData();
            rescheduleAppointmentRequestDTO.setSapPartnerId(customerProfile.getSapPartnerId());
            System.out.println("---- [Update Appointment] START ----" + customerId);
            System.out.println("Incoming Request: " + rescheduleAppointmentRequestDTO);

            // 1️⃣ Create Appointment entity
            Optional<Appointment> appointment = appointmentRepository.findById(rescheduleAppointmentRequestDTO.getAppointmentId());
            appointment.get().setCustomerId(customerId);
            appointment.get().setAppointmentDate(Timestamp.valueOf(rescheduleAppointmentRequestDTO.getSchedule()));
            appointment.get().setType("RESCHEDULE");
//            appointment.get().setStatus("RESCHEDULE");

            // 2️⃣ Call SAP Feign Client
            try {
                ResponseEntity<RescheduleAppointmentSapResponse> response =
                        sapFeignClientC4c.customerRescheduleAppointment(rescheduleAppointmentRequestDTO);

                System.out.println("C4C Response Status: " + response.getStatusCode());
                System.out.println("C4C Response Body: " + response.getBody());

                RescheduleAppointmentSapResponse sapResponse = response.getBody();

                // 3️⃣ Handle SAP response
                if (response.getStatusCode().is2xxSuccessful() && sapResponse != null) {
                    appointment.get().setC4CId(sapResponse.getAppointmentId());
                    System.out.println("Appointment updated in SAP with ID: " + sapResponse.getAppointmentId());
                } else {
                    appointment.get().setC4CId("FAIL");
                    System.out.println("⚠️ SAP appointment update failed or returned null.");
                }
            } catch (Exception exception) {
                System.out.println("Failed to Reschedule in C4C");
            }
            // 4️⃣ Save locally
            appointmentRepository.save(appointment.get());
            System.out.println("Appointment saved in local DB with ID: " + appointment.get().getId());

            // 5️⃣ Prepare return object
            ReturnObject returnObject = new ReturnObject();
            returnObject.setStatus(true);
            returnObject.setData(appointment.get());
            returnObject.setMessage(messageUtil.getMessage("appointment.updated.successfully"));

            System.out.println("---- [Update Appointment] END SUCCESS ----");
            return ResponseEntity.status(HttpStatus.CREATED).body(returnObject);

        } catch (Exception e) {
            System.out.println("❌ Exception during appointment creation: " + e.getMessage());
            e.printStackTrace();

            ReturnObject error = new ReturnObject();
            error.setStatus(false);
            error.setMessage("Failed to create appointment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    public ResponseEntity<ReturnObject<?>> cancelCustomerAppointment(Integer customerId, CancelAppointmentDTO
            cancelAppointmentDTO) {

        Optional<Appointment> optAppointment =
                appointmentRepository.getAppointmentByCustomerIdAndIdAndStatus(customerId, cancelAppointmentDTO.getAppId(), "OPEN");

        if (optAppointment.isEmpty()) {
            return ResponseEntity.ok(
                    new ReturnObject<>(
                            "No appointments found for this customer with this Id",
                            false,
                            Collections.emptyList()
                    )
            );
        }

        Appointment appointment = optAppointment.get();
        String c4cId = appointment.getC4CId();

        // Call SAP only when C4CId is valid
        boolean shouldCallSap =
                c4cId != null &&
                        !c4cId.equalsIgnoreCase("FAIL") &&
                        !c4cId.equalsIgnoreCase("SAP_EXCEPTION");

        if (shouldCallSap) {
            try {
                C4cCancelAppointmentDto c4cCancelAppointmentDto = new C4cCancelAppointmentDto();

                c4cCancelAppointmentDto.setAppId(c4cId);
                c4cCancelAppointmentDto.setNote("I dont need this appointment");
                c4cCancelAppointmentDto.setReasonCode("121");

                sapFeignClientC4c.userCancelAppointment(c4cCancelAppointmentDto);
            } catch (Exception ex) {
                System.out.println("SAP Appointment Cancel Error for C4CId " + c4cId + ex.getMessage());
            }
        }

        // update status regardless of SAP result
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


    public Optional<NotificationAppointmentDTO> getAppointmentById(Integer appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(a -> new NotificationAppointmentDTO(
                        a.getId(),
                        a.getZoomUrl(),
                        a.getZoomMeetingId()
                ));
    }
    public ReturnObject<HasAppointmentInfoDTO> getAppointmentByCustomerId(Long customerId) {
        Optional<Appointment> appointmentOptional = appointmentRepository.findTopByCustomerIdAndStartTimeIsNotNullOrderByStartTimeDesc(customerId);
        ReturnObject returnObject = new ReturnObject();
        if(appointmentOptional.isEmpty()){
            returnObject.setMessage("No Appointment Found");
            returnObject.setData(false);
            returnObject.setStatus(false);
            return returnObject;
        }
        Appointment appointment = appointmentOptional.get();
        HasAppointmentInfoDTO hasAppointmentInfoDTO = new HasAppointmentInfoDTO();
        hasAppointmentInfoDTO.setAppointmentId(appointment.getId());
        if(Objects.equals(appointment.getStatus(), "ON_CALL")){
            hasAppointmentInfoDTO.setHasAppointment(true);
        }else if (Objects.equals(appointment.getStatus(),"CLOSED") &&
                (appointment.getRate1() == null)){
            hasAppointmentInfoDTO.setIsRateEmpty(true);
            hasAppointmentInfoDTO.setHasAppointment(false);
        }else{
            hasAppointmentInfoDTO.setHasAppointment(false);
            hasAppointmentInfoDTO.setIsRateEmpty(false);
        }
        returnObject.setStatus(true);
        returnObject.setData(hasAppointmentInfoDTO);
        returnObject.setMessage("Appointment Returned Successfully");
        return returnObject;
    }

    public ReturnObject<?> getCustomerZoomData(Integer customerId) {
        Appointment appointment = appointmentRepository
                .findFirstByCustomerIdAndStatus(customerId, "ON_CALL")
                .orElse(null);

        if (appointment == null) {
            return new ReturnObject<>("No active call found for this customer", false, null);
        }

        if (appointment.getZoomMeetingId() == null) {
            return new ReturnObject<>("Zoom meeting is not yet initialized", false, null);
        }

        try {
            ResponseEntity<ReturnObject<Map<String, String>>> sigResponse =
                    communicationFeignClient.getCustomerSignature(appointment.getZoomMeetingId());

            if (!sigResponse.getStatusCode().is2xxSuccessful() || sigResponse.getBody() == null) {
                return new ReturnObject<>("Communication service unavailable", false, null);
            }

            Map<String, String> zoomData = sigResponse.getBody().getData();

            ObjectNode result = JsonNodeFactory.instance.objectNode();
            result.put("appointmentId", appointment.getId());
            result.put("meetingId", appointment.getZoomMeetingId());
            result.put("joinUrl", appointment.getZoomUrl());
            result.put("signature", zoomData.get("signature"));
            result.put("password", zoomData.get("password"));
            result.put("createdAt", zoomData.get("createdAt"));

            return new ReturnObject<>("Success", true, result);

        } catch (Exception e) {
            return new ReturnObject<>("Error: " + e.getMessage(), false, null);
        }
    }

    public ReturnObject<Boolean> isCustomerOnCall(Integer customerId) {

        boolean onCall = appointmentRepository
                .existsByCustomerIdAndStatus(customerId, "ON_CALL");

        if (!onCall) {
            return new ReturnObject<>("Customer is not on call", false, null);
        }

        return new ReturnObject<>("Customer is on call", true, null);
    }

    public ResponseEntity<?> appointmentRate(Long customerId, RatingDTO rateDto) {
        Appointment lastAppointment = appointmentRepository
                .findTopByCustomerIdAndStartTimeIsNotNullOrderByStartTimeDesc(customerId)
                .orElse(null);
        ReturnObject returnObject = new ReturnObject();
        if(lastAppointment!=null) {

            lastAppointment.setRate1(rateDto.getRate1());
            lastAppointment.setRate2(rateDto.getRate2());
            lastAppointment.setRate3(rateDto.getRate3());
            lastAppointment.setRate4(rateDto.getRate4());
            lastAppointment.setComment(rateDto.getComment());
            lastAppointment = appointmentRepository.save(lastAppointment);

            returnObject.setStatus(true);
            returnObject.setData(lastAppointment);
            returnObject.setMessage("Submitted Successfully");

            return ResponseEntity.status(HttpStatus.OK).body(returnObject);
        }else{
            returnObject.setStatus(false);
            returnObject.setData(null);
            returnObject.setMessage("No Appointment Found");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(returnObject);

        }
    }

    public ReturnObject<?> generateOtp(Integer customerId) {
        ReturnObject<Object> returnObject = new ReturnObject<>();

        try {
            AppointmentOtp recentOtp = appointmentOtpRepository
                    .findTopByCustomerIdOrderByCreatedAtDesc(customerId.longValue());

            if (recentOtp != null && recentOtp.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(3))) {
                returnObject.setStatus(false);
                returnObject.setMessage("OTP was already sent recently. Please wait before requesting a new one.");
                return returnObject;
            }

            String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));

            AppointmentOtp entity = new AppointmentOtp(
                    customerId.longValue(),
                    otp,
                    false,
                    LocalDateTime.now().plusMinutes(3),
                    LocalDateTime.now()
            );

            appointmentOtpRepository.save(entity);

            CustomerBasicInfoDTO customer =
                    customerMSFeignClient.getCustomerBasicInfo(customerId.longValue()).getData();

            String email = customer.getEmail();
            if (email != null && !email.isEmpty()) {
                sendOtpEmail(email, otp);
            } else {
                log.warn("No email found for customerId={}", customerId);
            }

            if (customer.getMobile() != null && !customer.getMobile().isEmpty() && "+20".equals(customer.getCountryCode())) {
                sendOtpSms(customer.getMobile(), otp);
            } else {
                log.warn("Skipping SMS for customerId={}, mobile missing or country code not +20", customerId);
            }

            returnObject.setStatus(true);
            returnObject.setMessage("OTP generated and sent successfully");
            return returnObject;

        } catch (Exception ex) {
            ex.printStackTrace();
            returnObject.setStatus(false);
            returnObject.setMessage("Failed to generate OTP: " + ex.getMessage());
            return returnObject;
        }
    }

    private void sendOtpSms(String mobile, String otp) {

        SingleSMSRequestDTO dto = new SingleSMSRequestDTO();
        dto.setMobile(mobile);
        dto.setContent("Your OTP is: " + otp);

        communicationFeignClient.sendSingleSms(dto);
    }

    private void sendOtpEmail(String email, String otp) {

        UserOtpMailDTO dto = new UserOtpMailDTO();
        dto.setEmail(email);
        dto.setMailSubject("Appointment OTP Verification");
        dto.setMailContent(
                "<p>Your OTP is <b>" + otp + "</b></p>"
        );

        communicationFeignClient.sendMail(dto);
    }


    public ReturnObject<Boolean> verifyOtp(Integer customerId, String otp) {
        ReturnObject<Boolean> returnObject = new ReturnObject<>();

        try {
            AppointmentOtp recentOtp = appointmentOtpRepository
                    .findTopByCustomerIdOrderByCreatedAtDesc(customerId.longValue());

            if (recentOtp == null) {
                returnObject.setStatus(false);
                returnObject.setMessage("No OTP found for this customer");
                returnObject.setData(false);
                return returnObject;
            }

            if (recentOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
                returnObject.setStatus(false);
                returnObject.setMessage("OTP has expired");
                returnObject.setData(false);
                return returnObject;
            }

            // 3️⃣ Check match
            if (!recentOtp.getOtp().equals(otp)) {
                returnObject.setStatus(false);
                returnObject.setMessage("OTP is incorrect");
                returnObject.setData(false);
                return returnObject;
            }

            recentOtp.setVerifiedAt(LocalDateTime.now());
            appointmentOtpRepository.save(recentOtp);

            returnObject.setStatus(true);
            returnObject.setMessage("OTP verified successfully");
            returnObject.setData(true);
            return returnObject;

        } catch (Exception ex) {
            ex.printStackTrace();
            returnObject.setStatus(false);
            returnObject.setMessage("Failed to verify OTP: " + ex.getMessage());
            returnObject.setData(false);
            return returnObject;
        }
    }


}

