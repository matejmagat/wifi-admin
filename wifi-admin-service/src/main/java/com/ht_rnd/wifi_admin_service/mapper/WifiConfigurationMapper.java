package com.ht_rnd.wifi_admin_service.mapper;

import com.ht_rnd.wifi_admin_service.entity.WifiConfigurationEntity;
import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.WifiConfigurationType;
import local.wifi_admin.platform.v1.WifiBandType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class WifiConfigurationMapper {

    public WifiConfiguration toDto(WifiConfigurationEntity entity) {
        WifiConfiguration dto = new WifiConfiguration();
        dto.setCpeId(entity.getCpeId());
        dto.setSsid(entity.getSsid());
        dto.setEncryptionType(entity.getEncryptionType());
        dto.setPassword(entity.getPassword());
        dto.setWifiBandType(entity.getWifiBandType());
        return dto;
    }

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

    public void updateEntityFromDto(WifiConfiguration dto, WifiConfigurationEntity entity) {
        entity.setCpeId(dto.getCpeId());
        entity.setSsid(dto.getSsid());
        entity.setEncryptionType(dto.getEncryptionType());
        entity.setPassword(dto.getPassword());
        entity.setWifiBandType(dto.getWifiBandType());
        entity.setLastSyncedAt(LocalDateTime.now());
    }

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