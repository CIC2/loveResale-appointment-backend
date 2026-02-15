package com.resale.homeflyappointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class VsoAppointmentBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(VsoAppointmentBackendApplication.class, args);
	}

}


