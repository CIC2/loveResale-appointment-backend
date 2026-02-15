package com.resale.homeflyappointment.feign;

import com.resale.homeflyappointment.components.customerAppointment.dto.CustomerValidationResultDTO;
import com.resale.homeflyappointment.components.customerAppointment.dto.ProfileResponseDTO;
import com.resale.homeflyappointment.components.teamLeadAppointment.dto.CustomerBasicInfoDTO;
import com.resale.homeflyappointment.utils.ReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "customer-service",
        url = "${customer.url}",
        configuration = CustomerFeignConfig.class
)
public interface CustomerFeignClient {

    // ✅ Get a single customer by ID
    @GetMapping("/id")
    ResponseEntity<ReturnObject<ProfileResponseDTO>> getCustomerProfile(
            @RequestParam("customerId") Long customerId
    );

    // ✅ Get a single customer by mobile and country code
    @GetMapping("/mobileAndCountryCode")
    ResponseEntity<ReturnObject<ProfileResponseDTO>> getCustomerProfileByMobileAndCountryCode(
            @RequestParam("mobile") Integer mobile,
            @RequestParam("countryCode") String countryCode
    );

    // ✅ NEW: Get multiple customers by IDs
    @GetMapping("/listByIds")
    ResponseEntity<ReturnObject<List<ProfileResponseDTO>>> getCustomersByIds(
            @RequestParam("customerIds") List<Long> customerIds,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "mobile", required = false) String mobile
//            @RequestHeader("X-Internal-Auth") String internalToken
    );

    @GetMapping("/validateFullyRegistered")
    ResponseEntity<ReturnObject<CustomerValidationResultDTO>> validateCustomer(
            @RequestParam("customerId") Long customerId
    );

    @GetMapping("/{customerId}")
    ResponseEntity<ReturnObject<CustomerBasicInfoDTO>> getCustomerBasicInfo(
            @PathVariable("customerId") Long customerId
    );

}


