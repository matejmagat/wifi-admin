package com.ht_rnd.wifi_admin_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import com.ht_rnd.wifi_admin_service.service.WifiService;
import jakarta.validation.ValidationException;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.WifiBandType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link WifiController}.
 *
 * <p>These tests boot only the Spring MVC slice (the controller + its
 * exception handlers) and mock the {@link WifiService} collaborator. They
 * verify that the REST endpoints defined in openapi.yaml
 * ({@code GET /wifi-parameter/{cpeId}} and {@code PUT /wifi-parameter})
 * return the correct HTTP status codes and JSON bodies for both the happy
 * path and each documented error case (400 / 404 / 502).</p>
 *
 * <p>NOTE on JSON field names: the REST model
 * {@link WifiConfiguration} exposes the band via the property
 * {@code wifiBandType} (getter {@code getWifiBandType}), so Jackson
 * serialises/deserialises it under the JSON key {@code "wifiBandType"}.
 * The openapi.yaml document names that field {@code wifiBand}; the tests
 * assert the actual runtime behaviour of the code, i.e. {@code wifiBandType}.</p>
 *
 * <p>NOTE on enum values: Spring Boot does not register the Jackson JAXB
 * annotation module by default, so the {@code @XmlEnumValue} annotations on
 * {@link EncryptionType} are ignored for JSON. Jackson therefore serialises
 * each enum by its Java constant name. Concretely {@code WPA_2_PSK} appears
 * in JSON as {@code "WPA_2_PSK"} (not {@code "WPA2_PSK"} as openapi.yaml
 * lists) and {@code WPA_3_SAE} as {@code "WPA_3_SAE"}. The assertions below
 * match the real behaviour. If you want the openapi.yaml values instead,
 * register {@code JakartaXmlBindAnnotationModule} on the ObjectMapper (or add
 * {@code @JsonProperty}) and update these expectations accordingly.</p>
 */
@WebMvcTest(WifiController.class)
class WifiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WifiService wifiService;

    private WifiConfiguration sampleConfig(String cpeId,
                                           WifiBandType band,
                                           String ssid,
                                           EncryptionType enc,
                                           String password) {
        WifiConfiguration c = new WifiConfiguration();
        c.setCpeId(cpeId);
        c.setWifiBandType(band);
        c.setSsid(ssid);
        c.setEncryptionType(enc);
        c.setPassword(password);
        return c;
    }

    // ---------------------------------------------------------------------
    // GET /wifi-parameter/{cpeId}
    // ---------------------------------------------------------------------

    @Test
    void get_returns200_withWifiConfiguration() throws Exception {
        WifiConfiguration cfg = sampleConfig(
                "CPE-1", WifiBandType.BAND_2_4_GHZ, "HomeNet",
                EncryptionType.WPA_2_PSK, "secret123");
        when(wifiService.getWifiParams("CPE-1")).thenReturn(cfg);

        mockMvc.perform(get("/wifi-parameter/{cpeId}", "CPE-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cpeId").value("CPE-1"))
                .andExpect(jsonPath("$.wifiBandType").value("BAND_2_4_GHZ"))
                .andExpect(jsonPath("$.ssid").value("HomeNet"))
                .andExpect(jsonPath("$.encryptionType").value("WPA_2_PSK"))
                .andExpect(jsonPath("$.password").value("secret123"));
    }

    @Test
    void get_openNetwork_returns200_withNullPassword() throws Exception {
        WifiConfiguration cfg = sampleConfig(
                "CPE-OPEN", WifiBandType.BAND_5_GHZ, "FreeWifi",
                EncryptionType.OPEN, null);
        when(wifiService.getWifiParams("CPE-OPEN")).thenReturn(cfg);

        mockMvc.perform(get("/wifi-parameter/{cpeId}", "CPE-OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpeId").value("CPE-OPEN"))
                .andExpect(jsonPath("$.wifiBandType").value("BAND_5_GHZ"))
                .andExpect(jsonPath("$.encryptionType").value("OPEN"))
                // Spring Boot's default Jackson config serialises nulls, so the
                // key is present with a JSON null rather than omitted.
                .andExpect(jsonPath("$.password").value(nullValue()));
    }

    @Test
    void get_unknownCpe_returns404_withErrorBody() throws Exception {
        when(wifiService.getWifiParams("UNKNOWN"))
                .thenThrow(new WifiService.CpeNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/wifi-parameter/{cpeId}", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CPE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("CPE not found: UNKNOWN"));
    }

    @Test
    void get_soapError_returns502_withErrorBody() throws Exception {
        when(wifiService.getWifiParams("CPE-BADGW"))
                .thenThrow(new WifiService.SoapCommunicationException(
                        "SOAP communication error: timeout", null));

        mockMvc.perform(get("/wifi-parameter/{cpeId}", "CPE-BADGW"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("SOAP_ERROR"))
                .andExpect(jsonPath("$.message").value("SOAP communication error: timeout"));
    }

    // ---------------------------------------------------------------------
    // PUT /wifi-parameter
    // ---------------------------------------------------------------------

    @Test
    void put_returns200_withConfirmedConfiguration() throws Exception {
        WifiConfiguration request = sampleConfig(
                "CPE-1", WifiBandType.BAND_5_GHZ, "MyNet",
                EncryptionType.WPA_3_SAE, "strongPass");
        // service echoes back the confirmed configuration
        when(wifiService.updateWifiParams(any(WifiConfiguration.class))).thenReturn(request);

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cpeId").value("CPE-1"))
                .andExpect(jsonPath("$.wifiBandType").value("BAND_5_GHZ"))
                .andExpect(jsonPath("$.ssid").value("MyNet"))
                .andExpect(jsonPath("$.encryptionType").value("WPA_3_SAE"))
                .andExpect(jsonPath("$.password").value("strongPass"));
    }

    @Test
    void put_missingRequiredField_returns400_fromBeanValidation() throws Exception {
        // ssid is @NotBlank in the REST model -> @Valid should reject the body.
        WifiConfiguration bad = sampleConfig(
                "CPE-1", WifiBandType.BAND_2_4_GHZ, "  ",
                EncryptionType.OPEN, null);

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_missingWifiBand_returns400_fromBeanValidation() throws Exception {
        // wifiBandType is @NotNull in the REST model.
        WifiConfiguration bad = sampleConfig(
                "CPE-1", null, "MyNet", EncryptionType.OPEN, null);

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_encryptionRequiresPassword_returns400_withValidationError() throws Exception {
        // Service throws jakarta ValidationException when WPA2_PSK has no password.
        WifiConfiguration request = sampleConfig(
                "CPE-1", WifiBandType.BAND_2_4_GHZ, "MyNet",
                EncryptionType.WPA_2_PSK, null);
        when(wifiService.updateWifiParams(any(WifiConfiguration.class)))
                .thenThrow(new ValidationException(
                        "Password is required for encryption type: WPA_2_PSK"));

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Password is required for encryption type: WPA_2_PSK"));
    }

    @Test
    void put_unknownCpe_returns404_withErrorBody() throws Exception {
        WifiConfiguration request = sampleConfig(
                "GHOST", WifiBandType.BAND_2_4_GHZ, "MyNet",
                EncryptionType.OPEN, null);
        when(wifiService.updateWifiParams(any(WifiConfiguration.class)))
                .thenThrow(new WifiService.CpeNotFoundException("GHOST"));

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CPE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("CPE not found: GHOST"));
    }

    @Test
    void put_soapError_returns502_withErrorBody() throws Exception {
        WifiConfiguration request = sampleConfig(
                "CPE-1", WifiBandType.BAND_2_4_GHZ, "MyNet",
                EncryptionType.OPEN, null);
        when(wifiService.updateWifiParams(any(WifiConfiguration.class)))
                .thenThrow(new WifiService.SoapCommunicationException(
                        "SOAP fault: backend down", null));

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("SOAP_ERROR"))
                .andExpect(jsonPath("$.message").value("SOAP fault: backend down"));
    }
}