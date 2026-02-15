package com.resale.homeflyappointment.feign;

import com.resale.homeflyappointment.components.userAppointment.dto.UserProfileResponseDTO;
import com.resale.homeflyappointment.utils.ReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "user-service",
        url = "${user.url}",
        configuration = CustomerFeignConfig.class)
public interface UserFeignClient {
    @GetMapping("/id")
    ResponseEntity<ReturnObject<UserProfileResponseDTO>> getUserProfile(
            @RequestParam("userId") Long userId);

    @GetMapping("/assignSalesmanRoundRobin")
    ResponseEntity<ReturnObject<?>> getAssignSalesmanRoundRobin(@RequestParam Integer projectId);

    @GetMapping("/zoomId/{userId}")
    ResponseEntity<ReturnObject<String>> getZoomIdForUser(@PathVariable Integer userId);

    @GetMapping("/{teamLeadId}/{userId}/assigned")
    ResponseEntity<ReturnObject> isUserAssignedToTeamLead(
            @PathVariable Integer teamLeadId,
            @PathVariable Integer userId
    );
}



