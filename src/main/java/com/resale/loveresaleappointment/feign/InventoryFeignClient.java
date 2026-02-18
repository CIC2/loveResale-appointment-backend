package com.resale.loveresaleappointment.feign;

import com.resale.loveresaleappointment.components.userAppointment.dto.ModelProjectDTO;
import com.resale.loveresaleappointment.utils.ReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "inventory-ms", url = "${inventory.ms.url}")
public interface InventoryFeignClient {

    @GetMapping("/internal/{modelId}")
    ResponseEntity<ReturnObject<ModelProjectDTO>> getModelProject(@PathVariable("modelId") Integer modelId);

}

