package com.ht_rnd.wifi_admin_service.exception;

import com.ht_rnd.wifi_admin_service.service.WifiService;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles the case where the requested CPE does not exist.
     *
     * @param e domain exception containing the missing CPE identifier
     * @return HTTP 404 response with a structured error payload
     */
    @ExceptionHandler(WifiService.CpeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(WifiService.CpeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage(), "code", "CPE_NOT_FOUND"));
    }

    /**
     * Handles validation failures thrown explicitly from the service layer.
     *
     * @param e validation exception describing the invalid request data
     * @return HTTP 400 response with a structured error payload
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(ValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage(), "code", "VALIDATION_ERROR"));
    }

    /**
     * Handles validation failures triggered by @Valid on request bodies.
     *
     * @param e exception containing field validation details
     * @return HTTP 400 response with a structured error payload
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message, "code", "VALIDATION_ERROR"));
    }

    /**
     * Handles SOAP communication and SOAP fault errors propagated from the service layer.
     *
     * @param e exception describing the SOAP communication problem
     * @return HTTP 502 response with a structured error payload
     */
    @ExceptionHandler(WifiService.SoapCommunicationException.class)
    public ResponseEntity<Map<String, String>> handleSoapError(WifiService.SoapCommunicationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", e.getMessage(), "code", "SOAP_ERROR"));
    }
}