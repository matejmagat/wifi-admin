package com.ht_rnd.wifi_admin_service.service;

import com.ht_rnd.wifi_admin_service.client.SoapClient;
import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.WifiBandType;
import jakarta.validation.ValidationException;
import local.wifi_admin.platform.v1.GetCpeIdResponse;
import local.wifi_admin.platform.v1.UpdateCpeIdResponse;
import local.wifi_admin.platform.v1.WifiConfigurationType;
import org.springframework.stereotype.Service;
import org.springframework.ws.soap.client.SoapFaultClientException;

@Service
public class WifiService {

    private final SoapClient soapClient;

    /**
     * Creates the service with the SOAP client dependency.
     *
     * @param soapClient client used to communicate with the SOAP backend
     */
    public WifiService(SoapClient soapClient) {
        this.soapClient = soapClient;
    }

    /**
     * Retrieves Wi-Fi parameters for the given CPE identifier.
     *
     * <p>This method calls the SOAP backend, extracts the SOAP configuration object,
     * and converts it into the REST model used by the controller. SOAP faults that
     * indicate a missing device are translated into {@code CpeNotFoundException},
     * while other failures are wrapped in {@code SoapCommunicationException}.</p>
     *
     * @param cpeId unique identifier of the target device
     * @return mapped Wi-Fi configuration for the requested device
     */
    public WifiConfiguration getWifiParams(String cpeId) {
        try {
            GetCpeIdResponse response = soapClient.getCpeId(cpeId);
            return mapToRest(response.getConfiguration());
        } catch (SoapFaultClientException e) {
            String msg = e.getFaultStringOrReason();
            if (msg != null && msg.toLowerCase().contains("not found")) {
                throw new CpeNotFoundException(cpeId);
            }
            throw new SoapCommunicationException("SOAP fault: " + msg, e);
        } catch (Exception e) {
            throw new SoapCommunicationException("SOAP communication error: " + e.getMessage(), e);
        }
    }

    /**
     * Updates Wi-Fi parameters after applying service-level validation rules.
     *
     * <p>If the encryption type requires a password, the password must be present.
     * If the encryption type is {@code OPEN}, any provided password is cleared before
     * the request is sent. The SOAP response is then mapped back to the REST model.</p>
     *
     * @param config requested Wi-Fi configuration update
     * @return updated Wi-Fi configuration returned by the SOAP backend
     * @throws ValidationException if the request violates business validation rules
     */
    public WifiConfiguration updateWifiParams(WifiConfiguration config) {
        // Validation encryption that requires a password
        if (config.getEncryptionType() != null
                && config.getEncryptionType() != EncryptionType.OPEN
                && (config.getPassword() == null || config.getPassword().isBlank())) {
            throw new ValidationException("Password is required for encryption type: " + config.getEncryptionType());
        }

        // OPEN can't have a password
        if (config.getEncryptionType() == EncryptionType.OPEN && config.getPassword() != null && !config.getPassword().isBlank()) {
            config.setPassword(null);
        }

        try {
            UpdateCpeIdResponse response = soapClient.updateCpeId(config);
            return mapToRest(response.getConfiguration());
        } catch (SoapFaultClientException e) {
            String msg = e.getFaultStringOrReason();
            if (msg != null && msg.toLowerCase().contains("not found")) {
                throw new CpeNotFoundException(config.getCpeId());
            }
            throw new SoapCommunicationException("SOAP fault: " + msg, e);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SoapCommunicationException("SOAP communication error: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a SOAP-generated Wi-Fi configuration object into the REST model.
     *
     * @param soap SOAP model returned by the backend service
     * @return REST model exposed by the API
     */
    private WifiConfiguration mapToRest(WifiConfigurationType soap) {
        WifiConfiguration config = new WifiConfiguration();
        config.setCpeId(soap.getCpeId());
        config.setWifiBandType(WifiBandType.valueOf(soap.getWifiBand().value()));
        config.setSsid(soap.getSsid());
        if (soap.getEncryptionType() != null) {
            config.setEncryptionType(EncryptionType.valueOf(soap.getEncryptionType().toString()));
        }
        config.setPassword(soap.getPassword());
        return config;
    }

    /**
     * Exception thrown when a requested CPE cannot be found in the SOAP backend.
     */
    public static class CpeNotFoundException extends RuntimeException {
        /**
         * Creates an exception for a missing CPE identifier.
         *
         * @param cpeId identifier of the missing device
         */
        public CpeNotFoundException(String cpeId) {
            super("CPE not found: " + cpeId);
        }
    }

    /**
     * Exception thrown when communication with the SOAP backend fails.
     */
    public static class SoapCommunicationException extends RuntimeException {
        /**
         * Creates an exception describing a SOAP communication failure.
         *
         * @param message human-readable error description
         * @param cause original exception thrown during SOAP processing
         */
        public SoapCommunicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
