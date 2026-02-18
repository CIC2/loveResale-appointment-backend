package com.resale.loveresaleappointment.shared;

import com.resale.loveresaleappointment.utils.ReturnObject;
import feign.FeignException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReturnObject<Void>> handleAllExceptions(Exception ex) {
        ReturnObject<Void> response = new ReturnObject<>();
        System.out.println("HandleAllExceptions : " + ex.getMessage());
        response.setStatus(false);
        response.setMessage("Something went wrong");
        response.setData(null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ReturnObject<Object>> handleFeignException(FeignException ex) {

        ReturnObject<Object> response = new ReturnObject<>();
        response.setStatus(false);

        // Default message
        String message = "External service error";

        // Try to extract error body from Feign response
        if (ex.contentUTF8() != null && !ex.contentUTF8().isEmpty()) {
            message = ex.contentUTF8();
        }

        response.setMessage(message);
        response.setData(null);

        System.out.println("FeignException : " + ex.getMessage());
        // Propagate real HTTP status from downstream service
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ReturnObject<Void>> handlePermissionDenied(PermissionDeniedException ex) {
        ReturnObject<Void> response = new ReturnObject<>();
        System.out.println("HandlePermissionDenied : " + ex.getMessage());
        response.setMessage(ex.getMessage());
        response.setStatus(false);
        response.setData(null);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReturnObject<Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        System.out.println("MethodArgumentNotValidException : " + ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        ReturnObject<Object> response = new ReturnObject<>();
        response.setStatus(false);
        response.setMessage("Validation failed");
        response.setData(errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler({ IllegalArgumentException.class })
    public ResponseEntity<ReturnObject<Void>> handleBadRequest(Exception ex) {

        System.out.println("Exception : " + ex.getMessage());
        ReturnObject<Void> response = new ReturnObject<>();
        response.setStatus(false);
        response.setMessage(ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ReturnObject<Void>> handleNotFound(ResourceNotFoundException ex) {

        System.out.println("ResourceNotFoundException : " + ex.getMessage());
        ReturnObject<Void> response = new ReturnObject<>();
        response.setStatus(false);
        response.setMessage(ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}


