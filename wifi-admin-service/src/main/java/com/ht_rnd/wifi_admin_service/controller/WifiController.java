package com.ht_rnd.wifi_admin_service.controller;

import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import com.ht_rnd.wifi_admin_service.service.WifiService;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/wifi-parameter")
public class WifiController {

    private final WifiService wifiService;

    /**
     * Creates the REST controller with the Wi-Fi service dependency.
     *
     * @param wifiService service responsible for business logic and SOAP communication
     */
    public WifiController(WifiService wifiService) {
        this.wifiService = wifiService;
    }


    /**
     * Returns Wi-Fi parameters for the specified CPE identifier.
     *
     * <p>This endpoint delegates the lookup to the service layer, which retrieves
     * the data from the SOAP backend and maps it into the REST response model.</p>
     *
     * @param cpeId unique identifier of the target CPE device
     * @return HTTP 200 response containing the Wi-Fi configuration
     */
    @GetMapping("/{cpeId}")
    public ResponseEntity<WifiConfiguration> get(@PathVariable String cpeId) {
        return ResponseEntity.ok(wifiService.getWifiParams(cpeId));
    }

    /**
     * Updates Wi-Fi parameters for a device.
     *
     * <p>The request body is validated before being passed to the service layer.
     * The service then applies business validation, forwards the update to the SOAP server,
     * and returns the updated configuration.</p>
     *
     * @param config validated Wi-Fi configuration received from the REST client
     * @return HTTP 200 response containing the updated Wi-Fi configuration
     */
    @PutMapping
    public ResponseEntity<WifiConfiguration> put(@Valid @RequestBody WifiConfiguration config) {
        return ResponseEntity.ok(wifiService.updateWifiParams(config));
    }

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
     * Handles validation failures for incoming REST requests or service-level rules.
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
