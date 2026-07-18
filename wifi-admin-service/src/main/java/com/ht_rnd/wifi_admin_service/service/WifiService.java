package com.ht_rnd.wifi_admin_service.service;

import com.ht_rnd.wifi_admin_service.client.SoapClient;
import com.ht_rnd.wifi_admin_service.entity.WifiConfigurationEntity;
import com.ht_rnd.wifi_admin_service.mapper.WifiConfigurationMapper;
import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import com.ht_rnd.wifi_admin_service.repository.WifiConfigurationRepository;
import jakarta.validation.ValidationException;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.GetCpeIdResponse;
import local.wifi_admin.platform.v1.UpdateCpeIdResponse;
import local.wifi_admin.platform.v1.WifiConfigurationType;
import org.springframework.stereotype.Service;
import org.springframework.ws.soap.client.SoapFaultClientException;

@Service
public class WifiService {

    private final SoapClient soapClient;
    private final WifiConfigurationRepository wifiConfigurationRepository;
    private final WifiConfigurationMapper wifiConfigurationMapper;

    /**
     * Creates the service with SOAP, repository, and mapping dependencies.
     *
     * @param soapClient SOAP client used to communicate with the upstream platform
     * @param wifiConfigurationRepository repository used for database access
     * @param wifiConfigurationMapper mapper used to convert between entity, SOAP, and REST models
     */
    public WifiService(
            SoapClient soapClient,
            WifiConfigurationRepository wifiConfigurationRepository,
            WifiConfigurationMapper wifiConfigurationMapper
    ) {
        this.soapClient = soapClient;
        this.wifiConfigurationRepository = wifiConfigurationRepository;
        this.wifiConfigurationMapper = wifiConfigurationMapper;
    }

    /**
     * Retrieves Wi-Fi parameters for the given CPE identifier.
     *
     * <p>The database is checked first. If a record exists, it is returned immediately.
     * If no record is found, the SOAP backend is queried, the result is stored in the
     * database, and the stored value is returned.</p>
     *
     * @param cpeId unique identifier of the target device
     * @return Wi-Fi configuration for the requested device
     */
    public WifiConfiguration getWifiParams(String cpeId) {
        return wifiConfigurationRepository.findByCpeId(cpeId)
                .map(wifiConfigurationMapper::toDto)
                .orElseGet(() -> fetchFromSoapAndSave(cpeId));
    }

    /**
     * Updates Wi-Fi parameters after applying service-level validation rules.
     *
     * <p>The update is first sent to the SOAP backend. If the SOAP update succeeds,
     * the returned configuration is written to the local database and then returned
     * to the REST layer.</p>
     *
     * @param config requested Wi-Fi configuration update
     * @return updated Wi-Fi configuration returned by the SOAP backend and persisted locally
     * @throws ValidationException if the request violates business validation rules
     */
    public WifiConfiguration updateWifiParams(WifiConfiguration config) {
        validateWifiConfiguration(config);

        try {
            UpdateCpeIdResponse response = soapClient.updateCpeId(config);
            WifiConfigurationType soapConfig = response.getConfiguration();

            WifiConfigurationEntity entity = wifiConfigurationRepository.findByCpeId(soapConfig.getCpeId())
                    .orElseGet(() -> wifiConfigurationMapper.toNewEntity(soapConfig));

            wifiConfigurationMapper.updateEntityFromSoap(soapConfig, entity);
            WifiConfigurationEntity savedEntity = wifiConfigurationRepository.save(entity);

            return wifiConfigurationMapper.toDto(savedEntity);
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
     * Fetches Wi-Fi configuration from the SOAP backend, saves it to the database,
     * and returns the persisted result as a REST DTO.
     *
     * @param cpeId unique identifier of the target device
     * @return persisted Wi-Fi configuration
     */
    private WifiConfiguration fetchFromSoapAndSave(String cpeId) {
        try {
            GetCpeIdResponse response = soapClient.getCpeId(cpeId);
            WifiConfigurationType soapConfig = response.getConfiguration();

            WifiConfigurationEntity entity = wifiConfigurationRepository.findByCpeId(cpeId)
                    .orElseGet(() -> wifiConfigurationMapper.toNewEntity(soapConfig));

            wifiConfigurationMapper.updateEntityFromSoap(soapConfig, entity);
            WifiConfigurationEntity savedEntity = wifiConfigurationRepository.save(entity);

            return wifiConfigurationMapper.toDto(savedEntity);
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
     * Applies business validation rules before updating Wi-Fi parameters.
     *
     * <p>Encrypted networks require a password. Open networks must not keep a password.</p>
     *
     * @param config requested Wi-Fi configuration update
     */
    private void validateWifiConfiguration(WifiConfiguration config) {
        if (config.getEncryptionType() != null
                && config.getEncryptionType() != EncryptionType.OPEN
                && (config.getPassword() == null || config.getPassword().isBlank())) {
            throw new ValidationException("Password is required for encryption type: " + config.getEncryptionType());
        }

        if (config.getEncryptionType() == EncryptionType.OPEN
                && config.getPassword() != null
                && !config.getPassword().isBlank()) {
            config.setPassword(null);
        }
    }

    /**
     * Exception thrown when a requested CPE cannot be found in the database or SOAP backend.
     */
    public static class CpeNotFoundException extends RuntimeException {
        public CpeNotFoundException(String cpeId) {
            super("CPE not found: " + cpeId);
        }
    }

    /**
     * Exception thrown when communication with the SOAP backend fails.
     */
    public static class SoapCommunicationException extends RuntimeException {
        public SoapCommunicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}