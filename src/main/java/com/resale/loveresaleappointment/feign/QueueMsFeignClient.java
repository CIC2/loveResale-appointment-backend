package com.resale.loveresaleappointment.feign;

import com.resale.loveresaleappointment.components.queueAppointment.dto.AppointmentInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "queue-ms", url = "${queue.ms.url}")
public interface QueueMsFeignClient {

    @PostMapping("/internal/newCall")
    void notifyNewCall(@RequestBody AppointmentInfoDTO dto);
}


