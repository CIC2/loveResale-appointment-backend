package com.resale.loveresaleappointment.feign;

import com.resale.loveresaleappointment.components.teamLeadAppointment.dto.CustomerBasicInfoDTO;
import com.resale.loveresaleappointment.utils.ReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;


@FeignClient(
            name = "customer-ms",
            url = "${customer.ms.url}"
    )

    public interface CustomerMSFeignClient {

            @GetMapping("/customerClient/{customerId}")
            ReturnObject<CustomerBasicInfoDTO> getCustomerBasicInfo(
            @PathVariable Long customerId
    );

        @PostMapping("/customerClient/batch/basicInfo")
        ReturnObject<List<CustomerBasicInfoDTO>> getCustomersBasicInfo(
                @RequestBody List<Long> customerIds
        );

    @GetMapping("/customerClient/emails")
    ReturnObject<List<Map<String, Object>>> getCustomerEmails();

    }




