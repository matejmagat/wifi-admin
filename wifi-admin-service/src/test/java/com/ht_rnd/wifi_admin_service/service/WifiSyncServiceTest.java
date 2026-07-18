package com.ht_rnd.wifi_admin_service.service;

import com.ht_rnd.wifi_admin_service.client.SoapClient;
import com.ht_rnd.wifi_admin_service.entity.WifiConfigurationEntity;
import com.ht_rnd.wifi_admin_service.mapper.WifiConfigurationMapper;
import com.ht_rnd.wifi_admin_service.repository.WifiConfigurationRepository;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.GetCpeIdResponse;
import local.wifi_admin.platform.v1.WifiBandType;
import local.wifi_admin.platform.v1.WifiConfigurationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WifiSyncServiceTest {

    @Mock
    private WifiConfigurationRepository wifiConfigurationRepository;

    @Mock
    private SoapClient soapClient;

    private WifiConfigurationMapper wifiConfigurationMapper;
    private WifiSyncService wifiSyncService;

    @BeforeEach
    void setUp() {
        wifiConfigurationMapper = new WifiConfigurationMapper();
        wifiSyncService = new WifiSyncService(
                wifiConfigurationRepository,
                wifiConfigurationMapper,
                soapClient
        );
    }

    private WifiConfigurationEntity entity(
            String cpeId,
            WifiBandType wifiBandType,
            String ssid,
            EncryptionType encryptionType,
            String password
    ) {
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId(cpeId);
        entity.setWifiBandType(wifiBandType);
        entity.setSsid(ssid);
        entity.setEncryptionType(encryptionType);
        entity.setPassword(password);
        entity.setLastSyncedAt(LocalDateTime.now().minusMinutes(10));
        return entity;
    }

    private WifiConfigurationType soapConfig(
            String cpeId,
            WifiBandType wifiBandType,
            String ssid,
            EncryptionType encryptionType,
            String password
    ) {
        WifiConfigurationType config = new WifiConfigurationType();
        config.setCpeId(cpeId);
        config.setWifiBand(wifiBandType);
        config.setSsid(ssid);
        config.setEncryptionType(encryptionType);
        config.setPassword(password);
        return config;
    }

    private SoapFaultClientException soapFault(String faultString) {
        SoapMessage message = mock(SoapMessage.class);
        SoapBody body = mock(SoapBody.class);
        SoapFault fault = mock(SoapFault.class);

        doReturn(faultString).when(message).getFaultReason();
        doReturn(body).when(message).getSoapBody();
        doReturn(fault).when(body).getFault();
        doReturn(faultString).when(fault).getFaultStringOrReason();

        return new SoapFaultClientException(message);
    }

    @Test
    void syncAllKnownWifiConfigurations_whenNoLocalRows_doesNothing() {
        when(wifiConfigurationRepository.findAll()).thenReturn(List.of());

        wifiSyncService.syncAllKnownWifiConfigurations();

        verify(wifiConfigurationRepository).findAll();
        verifyNoInteractions(soapClient);
        verify(wifiConfigurationRepository, never()).save(any());
    }

    @Test
    void syncAllKnownWifiConfigurations_whenSoapReturnsLatestData_updatesAndSavesEntity() {
        WifiConfigurationEntity existing = entity(
                "CPE-1",
                WifiBandType.BAND_2_4_GHZ,
                "OldNet",
                EncryptionType.WPA_2_PSK,
                "old-password"
        );

        when(wifiConfigurationRepository.findAll()).thenReturn(List.of(existing));

        GetCpeIdResponse response = new GetCpeIdResponse();
        response.setConfiguration(
                soapConfig(
                        "CPE-1",
                        WifiBandType.BAND_5_GHZ,
                        "NewNet",
                        EncryptionType.OPEN,
                        null
                )
        );

        when(soapClient.getCpeId("CPE-1")).thenReturn(response);
        when(wifiConfigurationRepository.save(any(WifiConfigurationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        wifiSyncService.syncAllKnownWifiConfigurations();

        ArgumentCaptor<WifiConfigurationEntity> captor =
                ArgumentCaptor.forClass(WifiConfigurationEntity.class);

        verify(wifiConfigurationRepository).findAll();
        verify(soapClient).getCpeId("CPE-1");
        verify(wifiConfigurationRepository).save(captor.capture());

        WifiConfigurationEntity saved = captor.getValue();
        assertEquals("CPE-1", saved.getCpeId());
        assertEquals(WifiBandType.BAND_5_GHZ, saved.getWifiBandType());
        assertEquals("NewNet", saved.getSsid());
        assertEquals(EncryptionType.OPEN, saved.getEncryptionType());
        assertNull(saved.getPassword());
        assertNotNull(saved.getLastSyncedAt());
    }

    @Test
    void syncAllKnownWifiConfigurations_whenOneSoapCallFails_continuesWithNextEntity() {
        WifiConfigurationEntity first = entity(
                "CPE-FAIL",
                WifiBandType.BAND_2_4_GHZ,
                "OldFail",
                EncryptionType.WPA_2_PSK,
                "fail-pass"
        );

        WifiConfigurationEntity second = entity(
                "CPE-OK",
                WifiBandType.BAND_2_4_GHZ,
                "OldOk",
                EncryptionType.WPA_2_PSK,
                "ok-pass"
        );

        when(wifiConfigurationRepository.findAll()).thenReturn(List.of(first, second));
        SoapFaultClientException fault = soapFault("platform unavailable");
        when(soapClient.getCpeId("CPE-FAIL")).thenThrow(fault);

        GetCpeIdResponse okResponse = new GetCpeIdResponse();
        okResponse.setConfiguration(
                soapConfig(
                        "CPE-OK",
                        WifiBandType.BAND_5_GHZ,
                        "FreshNet",
                        EncryptionType.WPA_3_SAE,
                        "fresh-pass"
                )
        );
        when(soapClient.getCpeId("CPE-OK")).thenReturn(okResponse);

        when(wifiConfigurationRepository.save(any(WifiConfigurationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        wifiSyncService.syncAllKnownWifiConfigurations();

        ArgumentCaptor<WifiConfigurationEntity> captor =
                ArgumentCaptor.forClass(WifiConfigurationEntity.class);

        verify(soapClient).getCpeId("CPE-FAIL");
        verify(soapClient).getCpeId("CPE-OK");
        verify(wifiConfigurationRepository, times(1)).save(captor.capture());

        WifiConfigurationEntity saved = captor.getValue();
        assertEquals("CPE-OK", saved.getCpeId());
        assertEquals(WifiBandType.BAND_5_GHZ, saved.getWifiBandType());
        assertEquals("FreshNet", saved.getSsid());
        assertEquals(EncryptionType.WPA_3_SAE, saved.getEncryptionType());
        assertEquals("fresh-pass", saved.getPassword());
    }

    @Test
    void syncAllKnownWifiConfigurations_whenSoapThrowsRuntimeException_continuesWithNextEntity() {
        WifiConfigurationEntity first = entity(
                "CPE-TIMEOUT",
                WifiBandType.BAND_2_4_GHZ,
                "TimeoutNet",
                EncryptionType.WPA_2_PSK,
                "timeout-pass"
        );

        WifiConfigurationEntity second = entity(
                "CPE-OK",
                WifiBandType.BAND_2_4_GHZ,
                "StableNet",
                EncryptionType.WPA_2_PSK,
                "stable-pass"
        );

        when(wifiConfigurationRepository.findAll()).thenReturn(List.of(first, second));
        when(soapClient.getCpeId("CPE-TIMEOUT")).thenThrow(new RuntimeException("read timed out"));

        GetCpeIdResponse okResponse = new GetCpeIdResponse();
        okResponse.setConfiguration(
                soapConfig(
                        "CPE-OK",
                        WifiBandType.BAND_2_4_GHZ,
                        "StableNetUpdated",
                        EncryptionType.WPA_2_PSK,
                        "stable-pass-2"
                )
        );
        when(soapClient.getCpeId("CPE-OK")).thenReturn(okResponse);

        when(wifiConfigurationRepository.save(any(WifiConfigurationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        wifiSyncService.syncAllKnownWifiConfigurations();

        ArgumentCaptor<WifiConfigurationEntity> captor =
                ArgumentCaptor.forClass(WifiConfigurationEntity.class);

        verify(soapClient).getCpeId("CPE-TIMEOUT");
        verify(soapClient).getCpeId("CPE-OK");
        verify(wifiConfigurationRepository, times(1)).save(captor.capture());

        WifiConfigurationEntity saved = captor.getValue();
        assertEquals("CPE-OK", saved.getCpeId());
        assertEquals("StableNetUpdated", saved.getSsid());
        assertEquals("stable-pass-2", saved.getPassword());
    }
}