package com.ht_rnd.wifi_admin_service.controller;

import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import com.ht_rnd.wifi_admin_service.service.WifiService;
import jakarta.validation.Valid;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.WifiBandType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
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
     * Returns all supported encryption types.
     *
     * @return HTTP 200 response containing all available encryption type values
     */
    @GetMapping("/encryption_type")
    public ResponseEntity<List<String>> getEncryptionTypes() {
        List<String> encryptionTypes = Arrays.stream(EncryptionType.values())
                .map(Enum::name)
                .toList();

        return ResponseEntity.ok(encryptionTypes);
    }

    /**
     * Returns all supported Wi-Fi band types.
     *
     * @return HTTP 200 response containing all available Wi-Fi band type values
     */
    @GetMapping("/wifi_band_type")
    public ResponseEntity<List<String>> getWifiBandTypes() {
        List<String> wifiBandTypes = Arrays.stream(WifiBandType.values())
                .map(Enum::name)
                .toList();

        return ResponseEntity.ok(wifiBandTypes);
    }
}
