package com.ht_rnd.wifi_admin_service.mapper;

import com.ht_rnd.wifi_admin_service.entity.WifiConfigurationEntity;
import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.WifiConfigurationType;
import local.wifi_admin.platform.v1.WifiBandType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Maps Wi-Fi configuration data between REST models, persistence entities,
 * and SOAP-generated types.
 * This component centralizes conversion logic so the service layer can work
 * with the appropriate model for each integration boundary.
 */
@Component
public class WifiConfigurationMapper {

    /**
     * Converts a persistence entity into the REST response model.
     *
     * @param entity persisted Wi-Fi configuration entity
     * @return REST model containing the mapped Wi-Fi configuration
     */
    public WifiConfiguration toDto(WifiConfigurationEntity entity) {
        WifiConfiguration dto = new WifiConfiguration();
        dto.setCpeId(entity.getCpeId());
        dto.setSsid(entity.getSsid());
        dto.setEncryptionType(entity.getEncryptionType());
        dto.setPassword(entity.getPassword());
        dto.setWifiBandType(entity.getWifiBandType());
        return dto;
    }

    /**
     * Creates a new persistence entity from the REST-layer Wi-Fi configuration.
     * The synchronization timestamp is initialized to the current time when
     * the entity is created.
     *
     * @param dto REST model containing Wi-Fi configuration data
     * @return new persistence entity populated from the REST model
     */
    public WifiConfigurationEntity toNewEntity(WifiConfiguration dto) {
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId(dto.getCpeId());
        entity.setSsid(dto.getSsid());
        entity.setEncryptionType(dto.getEncryptionType());
        entity.setPassword(dto.getPassword());
        entity.setWifiBandType(dto.getWifiBandType());
        entity.setLastSyncedAt(LocalDateTime.now());
        return entity;
    }

    /**
     * Updates an existing persistence entity using values from the REST model.
     * The synchronization timestamp is refreshed to reflect the update time.
     *
     * @param dto REST model containing the new Wi-Fi configuration values
     * @param entity existing persistence entity to update
     */
    public void updateEntityFromDto(WifiConfiguration dto, WifiConfigurationEntity entity) {
        entity.setCpeId(dto.getCpeId());
        entity.setSsid(dto.getSsid());
        entity.setEncryptionType(dto.getEncryptionType());
        entity.setPassword(dto.getPassword());
        entity.setWifiBandType(dto.getWifiBandType());
        entity.setLastSyncedAt(LocalDateTime.now());
    }

    /**
     * Creates a new persistence entity from the SOAP-generated Wi-Fi configuration type.
     * SOAP enum values are converted into the application's corresponding enum types.
     * The synchronization timestamp is initialized to the current time.
     *
     * @param soap SOAP-generated Wi-Fi configuration object
     * @return new persistence entity populated from the SOAP model
     */
    public WifiConfigurationEntity toNewEntity(WifiConfigurationType soap) {
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId(soap.getCpeId());
        entity.setSsid(soap.getSsid());
        entity.setPassword(soap.getPassword());
        entity.setWifiBandType(WifiBandType.valueOf(soap.getWifiBand().value()));

        if (soap.getEncryptionType() != null) {
            entity.setEncryptionType(EncryptionType.valueOf(soap.getEncryptionType().toString()));
        }

        entity.setLastSyncedAt(LocalDateTime.now());
        return entity;
    }

    /**
     * Updates an existing persistence entity using values received from the SOAP layer.
     * If the SOAP payload does not include an encryption type, the stored value is
     * cleared. The synchronization timestamp is refreshed after the update.
     *
     * @param soap SOAP-generated Wi-Fi configuration object
     * @param entity existing persistence entity to update
     */
    public void updateEntityFromSoap(WifiConfigurationType soap, WifiConfigurationEntity entity) {
        entity.setCpeId(soap.getCpeId());
        entity.setSsid(soap.getSsid());
        entity.setPassword(soap.getPassword());
        entity.setWifiBandType(WifiBandType.valueOf(soap.getWifiBand().value()));

        if (soap.getEncryptionType() != null) {
            entity.setEncryptionType(EncryptionType.valueOf(soap.getEncryptionType().toString()));
        } else {
            entity.setEncryptionType(null);
        }

        entity.setLastSyncedAt(LocalDateTime.now());
    }
}