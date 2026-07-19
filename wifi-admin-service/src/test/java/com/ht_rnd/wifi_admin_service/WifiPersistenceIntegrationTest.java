package com.ht_rnd.wifi_admin_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht_rnd.wifi_admin_service.client.SoapClient;
import com.ht_rnd.wifi_admin_service.entity.WifiConfigurationEntity;
import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import com.ht_rnd.wifi_admin_service.repository.WifiConfigurationRepository;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.GetCpeIdResponse;
import local.wifi_admin.platform.v1.UpdateCpeIdResponse;
import local.wifi_admin.platform.v1.WifiBandType;
import local.wifi_admin.platform.v1.WifiConfigurationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Security is disabled here (addFilters = false) because this test drives the
// real REST -> service -> repository stack over MockMvc without authenticating,
// and SecurityConfig requires an authenticated user for /wifi-parameter/**.
// Without this, every request below would get a 401 from Spring Security before
// ever reaching the controller.
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class WifiPersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WifiConfigurationRepository wifiConfigurationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SoapClient soapClient;

    @BeforeEach
    void setUp() {
        wifiConfigurationRepository.deleteAll();
        reset(soapClient);
    }

    private WifiConfigurationType soapConfig(
            String cpeId,
            WifiBandType band,
            String ssid,
            EncryptionType enc,
            String password
    ) {
        WifiConfigurationType c = new WifiConfigurationType();
        c.setCpeId(cpeId);
        c.setWifiBand(band);
        c.setSsid(ssid);
        c.setEncryptionType(enc);
        c.setPassword(password);
        return c;
    }

    private WifiConfiguration restConfig(
            String cpeId,
            WifiBandType band,
            String ssid,
            EncryptionType enc,
            String password
    ) {
        WifiConfiguration c = new WifiConfiguration();
        c.setCpeId(cpeId);
        c.setWifiBandType(band);
        c.setSsid(ssid);
        c.setEncryptionType(enc);
        c.setPassword(password);
        return c;
    }

    @Test
    void get_whenNotInDb_callsSoap_insertsRow_andReturnsJson() throws Exception {
        GetCpeIdResponse response = new GetCpeIdResponse();
        response.setConfiguration(
                soapConfig("CPE-1", WifiBandType.BAND_2_4_GHZ, "HomeNet", EncryptionType.WPA_2_PSK, "secret123")
        );

        when(soapClient.getCpeId("CPE-1")).thenReturn(response);

        mockMvc.perform(get("/wifi-parameter/{cpeId}", "CPE-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cpeId").value("CPE-1"))
                .andExpect(jsonPath("$.wifiBandType").value("BAND_2_4_GHZ"))
                .andExpect(jsonPath("$.ssid").value("HomeNet"))
                .andExpect(jsonPath("$.encryptionType").value("WPA_2_PSK"))
                .andExpect(jsonPath("$.password").value("secret123"));

        Optional<WifiConfigurationEntity> saved = wifiConfigurationRepository.findByCpeId("CPE-1");
        assertTrue(saved.isPresent());
        assertEquals("CPE-1", saved.get().getCpeId());
        assertEquals(WifiBandType.BAND_2_4_GHZ, saved.get().getWifiBandType());
        assertEquals("HomeNet", saved.get().getSsid());
        assertEquals(EncryptionType.WPA_2_PSK, saved.get().getEncryptionType());
        assertEquals("secret123", saved.get().getPassword());
        assertNotNull(saved.get().getLastSyncedAt());

        verify(soapClient, times(1)).getCpeId("CPE-1");
    }

    @Test
    void get_whenAlreadyInDb_returnsFromDb_withoutCallingSoapAgain() throws Exception {
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId("CPE-DB");
        entity.setWifiBandType(WifiBandType.BAND_5_GHZ);
        entity.setSsid("CachedWifi");
        entity.setEncryptionType(EncryptionType.OPEN);
        entity.setPassword(null);
        wifiConfigurationRepository.save(entity);

        mockMvc.perform(get("/wifi-parameter/{cpeId}", "CPE-DB"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cpeId").value("CPE-DB"))
                .andExpect(jsonPath("$.wifiBandType").value("BAND_5_GHZ"))
                .andExpect(jsonPath("$.ssid").value("CachedWifi"))
                .andExpect(jsonPath("$.encryptionType").value("OPEN"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verifyNoInteractions(soapClient);
    }

    @Test
    void get_secondCall_returnsCachedValue_andSoapIsCalledOnlyOnce() throws Exception {
        GetCpeIdResponse response = new GetCpeIdResponse();
        response.setConfiguration(
                soapConfig("CPE-CACHE", WifiBandType.BAND_2_4_GHZ, "CacheNet", EncryptionType.WPA_2_PSK, "cache-pass")
        );

        when(soapClient.getCpeId("CPE-CACHE")).thenReturn(response);

        mockMvc.perform(get("/wifi-parameter/{cpeId}", "CPE-CACHE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpeId").value("CPE-CACHE"))
                .andExpect(jsonPath("$.ssid").value("CacheNet"));

        mockMvc.perform(get("/wifi-parameter/{cpeId}", "CPE-CACHE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpeId").value("CPE-CACHE"))
                .andExpect(jsonPath("$.ssid").value("CacheNet"));

        verify(soapClient, times(1)).getCpeId("CPE-CACHE");
        assertEquals(1, wifiConfigurationRepository.count());
    }

    @Test
    void put_whenExistingCpe_updatesSoap_andUpdatesDbRow() throws Exception {
        WifiConfigurationEntity existing = new WifiConfigurationEntity();
        existing.setCpeId("CPE-1");
        existing.setWifiBandType(WifiBandType.BAND_2_4_GHZ);
        existing.setSsid("OldNet");
        existing.setEncryptionType(EncryptionType.WPA_2_PSK);
        existing.setPassword("oldpass");
        wifiConfigurationRepository.save(existing);

        UpdateCpeIdResponse response = new UpdateCpeIdResponse();
        response.setConfiguration(
                soapConfig("CPE-1", WifiBandType.BAND_5_GHZ, "NewNet", EncryptionType.OPEN, null)
        );

        when(soapClient.updateCpeId(any(WifiConfiguration.class))).thenReturn(response);

        WifiConfiguration request = restConfig(
                "CPE-1",
                WifiBandType.BAND_5_GHZ,
                "NewNet",
                EncryptionType.OPEN,
                null
        );

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpeId").value("CPE-1"))
                .andExpect(jsonPath("$.wifiBandType").value("BAND_5_GHZ"))
                .andExpect(jsonPath("$.ssid").value("NewNet"))
                .andExpect(jsonPath("$.encryptionType").value("OPEN"));

        Optional<WifiConfigurationEntity> updated = wifiConfigurationRepository.findByCpeId("CPE-1");
        assertTrue(updated.isPresent());
        assertEquals("NewNet", updated.get().getSsid());
        assertEquals(WifiBandType.BAND_5_GHZ, updated.get().getWifiBandType());
        assertEquals(EncryptionType.OPEN, updated.get().getEncryptionType());
        assertNull(updated.get().getPassword());
        assertNotNull(updated.get().getLastSyncedAt());

        verify(soapClient, times(1)).updateCpeId(any(WifiConfiguration.class));
    }
}