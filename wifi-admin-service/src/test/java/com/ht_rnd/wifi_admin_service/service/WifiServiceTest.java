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
import local.wifi_admin.platform.v1.WifiBandType;
import local.wifi_admin.platform.v1.WifiConfigurationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WifiServiceTest {

    @Mock
    private SoapClient soapClient;

    @Mock
    private WifiConfigurationRepository wifiConfigurationRepository;

    private WifiConfigurationMapper wifiConfigurationMapper;

    @InjectMocks
    private WifiService wifiService;

    @BeforeEach
    void setUp() {
        wifiConfigurationMapper = new WifiConfigurationMapper();
        wifiService = new WifiService(soapClient, wifiConfigurationRepository, wifiConfigurationMapper);
    }

    private WifiConfigurationEntity entity(
            String cpeId,
            WifiBandType band,
            String ssid,
            EncryptionType encryptionType,
            String password
    ) {
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId(cpeId);
        entity.setWifiBandType(band);
        entity.setSsid(ssid);
        entity.setEncryptionType(encryptionType);
        entity.setPassword(password);
        entity.setLastSyncedAt(LocalDateTime.now());
        return entity;
    }

    private WifiConfigurationType soapConfig(
            String cpeId,
            WifiBandType band,
            String ssid,
            EncryptionType encryptionType,
            String password
    ) {
        WifiConfigurationType config = new WifiConfigurationType();
        config.setCpeId(cpeId);
        config.setWifiBand(band);
        config.setSsid(ssid);
        config.setEncryptionType(encryptionType);
        config.setPassword(password);
        return config;
    }

    private WifiConfiguration restConfig(
            String cpeId,
            WifiBandType band,
            String ssid,
            EncryptionType encryptionType,
            String password
    ) {
        WifiConfiguration config = new WifiConfiguration();
        config.setCpeId(cpeId);
        config.setWifiBandType(band);
        config.setSsid(ssid);
        config.setEncryptionType(encryptionType);
        config.setPassword(password);
        return config;
    }

    @Test
    void getWifiParams_dbMiss_callsSoap_savesEntity_andReturnsDto() {
        String cpeId = "CPE-001";

        when(wifiConfigurationRepository.findByCpeId(cpeId)).thenReturn(Optional.empty());

        GetCpeIdResponse soapResponse = new GetCpeIdResponse();
        soapResponse.setConfiguration(
                soapConfig(cpeId, WifiBandType.BAND_2_4_GHZ, "Office-2G", EncryptionType.WPA_2_PSK, "seed-wifi-01")
        );
        when(soapClient.getCpeId(cpeId)).thenReturn(soapResponse);

        when(wifiConfigurationRepository.save(any(WifiConfigurationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WifiConfiguration result = wifiService.getWifiParams(cpeId);

        assertNotNull(result);
        assertEquals("CPE-001", result.getCpeId());
        assertEquals(WifiBandType.BAND_2_4_GHZ, result.getWifiBandType());
        assertEquals("Office-2G", result.getSsid());
        assertEquals(EncryptionType.WPA_2_PSK, result.getEncryptionType());
        assertEquals("seed-wifi-01", result.getPassword());

        ArgumentCaptor<WifiConfigurationEntity> entityCaptor =
                ArgumentCaptor.forClass(WifiConfigurationEntity.class);
        verify(wifiConfigurationRepository).save(entityCaptor.capture());

        WifiConfigurationEntity savedEntity = entityCaptor.getValue();
        assertEquals("CPE-001", savedEntity.getCpeId());
        assertEquals(WifiBandType.BAND_2_4_GHZ, savedEntity.getWifiBandType());
        assertEquals("Office-2G", savedEntity.getSsid());
        assertEquals(EncryptionType.WPA_2_PSK, savedEntity.getEncryptionType());
        assertEquals("seed-wifi-01", savedEntity.getPassword());
        assertNotNull(savedEntity.getLastSyncedAt());

        verify(soapClient).getCpeId(cpeId);
    }

    @Test
    void getWifiParams_dbHit_returnsEntityWithoutCallingSoap() {
        String cpeId = "CPE-001";

        WifiConfigurationEntity entity = entity(
                cpeId,
                WifiBandType.BAND_5_GHZ,
                "Office-5G",
                EncryptionType.OPEN,
                null
        );

        when(wifiConfigurationRepository.findByCpeId(cpeId)).thenReturn(Optional.of(entity));

        WifiConfiguration result = wifiService.getWifiParams(cpeId);

        assertNotNull(result);
        assertEquals("CPE-001", result.getCpeId());
        assertEquals(WifiBandType.BAND_5_GHZ, result.getWifiBandType());
        assertEquals("Office-5G", result.getSsid());
        assertEquals(EncryptionType.OPEN, result.getEncryptionType());
        assertNull(result.getPassword());

        verify(wifiConfigurationRepository, never()).save(any());
        verifyNoInteractions(soapClient);
    }

    @Test
    void updateWifiParams_existingCpe_callsSoap_updatesDb_andReturnsDto() {
        WifiConfiguration request = restConfig(
                "CPE-001",
                WifiBandType.BAND_2_4_GHZ,
                "Office-2G",
                EncryptionType.OPEN,
                null
        );

        WifiConfigurationEntity existingEntity = entity(
                "CPE-001",
                WifiBandType.BAND_5_GHZ,
                "Old-SSID",
                EncryptionType.WPA_2_PSK,
                "old-password"
        );

        when(wifiConfigurationRepository.findByCpeId("CPE-001"))
                .thenReturn(Optional.of(existingEntity));

        UpdateCpeIdResponse soapResponse = new UpdateCpeIdResponse();
        soapResponse.setConfiguration(
                soapConfig("CPE-001", WifiBandType.BAND_2_4_GHZ, "Office-2G", EncryptionType.OPEN, null)
        );
        when(soapClient.updateCpeId(request)).thenReturn(soapResponse);

        when(wifiConfigurationRepository.save(any(WifiConfigurationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WifiConfiguration result = wifiService.updateWifiParams(request);

        assertNotNull(result);
        assertEquals("CPE-001", result.getCpeId());
        assertEquals(WifiBandType.BAND_2_4_GHZ, result.getWifiBandType());
        assertEquals("Office-2G", result.getSsid());
        assertEquals(EncryptionType.OPEN, result.getEncryptionType());
        assertNull(result.getPassword());

        ArgumentCaptor<WifiConfigurationEntity> entityCaptor =
                ArgumentCaptor.forClass(WifiConfigurationEntity.class);
        verify(wifiConfigurationRepository).save(entityCaptor.capture());

        WifiConfigurationEntity savedEntity = entityCaptor.getValue();
        assertEquals("CPE-001", savedEntity.getCpeId());
        assertEquals(WifiBandType.BAND_2_4_GHZ, savedEntity.getWifiBandType());
        assertEquals("Office-2G", savedEntity.getSsid());
        assertEquals(EncryptionType.OPEN, savedEntity.getEncryptionType());
        assertNull(savedEntity.getPassword());
        assertNotNull(savedEntity.getLastSyncedAt());

        verify(soapClient).updateCpeId(request);
    }

    @Test
    void updateWifiParams_securedNetworkWithoutPassword_throwsValidationException() {
        WifiConfiguration request = restConfig(
                "CPE-001",
                WifiBandType.BAND_2_4_GHZ,
                "Office-2G",
                EncryptionType.WPA_2_PSK,
                null
        );

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> wifiService.updateWifiParams(request)
        );

        assertEquals("Password is required for encryption type: WPA_2_PSK", ex.getMessage());
        verifyNoInteractions(soapClient);
        verify(wifiConfigurationRepository, never()).save(any());
    }

    @Test
    void getWifiParams_soapNotFound_throwsCpeNotFoundException() {
        String cpeId = "DOES_NOT_EXIST";

        when(wifiConfigurationRepository.findByCpeId(cpeId)).thenReturn(Optional.empty());

        SoapFaultClientException fault = mock(SoapFaultClientException.class);
        when(fault.getFaultStringOrReason()).thenReturn("CPE not found on platform");
        when(soapClient.getCpeId(cpeId)).thenThrow(fault);

        WifiService.CpeNotFoundException ex = assertThrows(
                WifiService.CpeNotFoundException.class,
                () -> wifiService.getWifiParams(cpeId)
        );

        assertEquals("CPE not found: DOES_NOT_EXIST", ex.getMessage());
        verify(wifiConfigurationRepository, never()).save(any());
    }
}