package com.resale.loveresaleappointment.utils;

import com.resale.loveresaleappointment.components.customerAppointment.dto.CustomerValidationResultDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CustomerValidationService {


    public ReturnObject<CustomerValidationResultDTO> extractCustomerValidationError(
            ResponseEntity<ReturnObject<CustomerValidationResultDTO>> response
    ) {
        if (response == null || response.getBody() == null) {
            // return a unified error response (still no exception)
            ReturnObject<CustomerValidationResultDTO> error = new ReturnObject<>();
            error.setStatus(false);
            error.setMessage("Customer validation failed: empty response");
            return error;
        }

        ReturnObject<CustomerValidationResultDTO> body = response.getBody();

        if (!body.getStatus() &&
                body.getData() != null &&
                body.getData().getMissingFields() != null &&
                !body.getData().getMissingFields().isEmpty()) {

            return body;  // return the reasons to the caller
        }

        // Validation passed = no missing fields
        return null;
    }
}

