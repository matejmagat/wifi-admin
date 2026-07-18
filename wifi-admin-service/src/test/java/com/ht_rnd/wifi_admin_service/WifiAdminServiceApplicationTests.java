package com.ht_rnd.wifi_admin_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht_rnd.wifi_admin_service.client.SoapClient;
import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.GetCpeIdResponse;
import local.wifi_admin.platform.v1.UpdateCpeIdResponse;
import local.wifi_admin.platform.v1.WifiBandType;
import local.wifi_admin.platform.v1.WifiConfigurationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration tests that boot the entire Spring Boot application
 * context (controller + service + Spring MVC) but replace the
 * {@link SoapClient} bean with a Mockito mock.
 *
 * <p>This lets us drive the real {@code WifiController} -> {@code WifiService}
 * logic end-to-end over HTTP (via MockMvc) while controlling what the SOAP
 * layer "returns". It exercises:</p>
 * <ul>
 *   <li>the SOAP-response -> REST-model mapping in {@code WifiService.mapToRest};</li>
 *   <li>the service-level validation rules (password required for
 *       non-OPEN encryption, OPEN clears any password);</li>
 *   <li>the translation of SOAP faults into HTTP 404 / 502 as documented in
 *       openapi.yaml.</li>
 * </ul>
 *
 * <p>The real SOAP mock server is never contacted here, so these tests run
 * without it. (When you run locally with the mock SOAP server up, the app
 * itself will talk to it; these tests intentionally isolate the REST layer.)</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class WifiEndpointIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private SoapClient soapClient;

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private WifiConfigurationType soapConfig(String cpeId,
											 WifiBandType band,
											 String ssid,
											 EncryptionType enc,
											 String password) {
		WifiConfigurationType c = new WifiConfigurationType();
		c.setCpeId(cpeId);
		c.setWifiBand(band);
		c.setSsid(ssid);
		c.setEncryptionType(enc);
		c.setPassword(password);
		return c;
	}

	private WifiConfiguration restConfig(String cpeId,
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

	/**
	 * Builds a real {@link SoapFaultClientException} whose fault string is
	 * {@code faultString}, matching how {@code SoapClientInterceptor} would
	 * raise one on a SOAP fault. {@code WifiService} inspects
	 * {@code getFaultStringOrReason()} to decide between 404 and 502.
	 */
	private SoapFaultClientException soapFault(String faultString) {
		SoapMessage message = org.mockito.Mockito.mock(SoapMessage.class);
		SoapBody body = org.mockito.Mockito.mock(SoapBody.class);
		SoapFault fault = org.mockito.Mockito.mock(SoapFault.class);
		// Use doReturn(...).when(...) style: it does not leave Mockito in the
		// "unfinished stubbing" state that when(...).thenReturn(...) does, so this
		// helper is safe to call even from inside another stubbing expression.
		// WebServiceFaultException(FaultAwareWebServiceMessage) calls getFaultReason()
		// in its constructor; SoapFaultClientException then reads the SoapFault.
		org.mockito.Mockito.doReturn(faultString).when(message).getFaultReason();
		org.mockito.Mockito.doReturn(body).when(message).getSoapBody();
		org.mockito.Mockito.doReturn(fault).when(body).getFault();
		org.mockito.Mockito.doReturn(faultString).when(fault).getFaultStringOrReason();
		return new SoapFaultClientException(message);
	}

	// ------------------------------------------------------------------
	// GET /wifi-parameter/{cpeId}
	// ------------------------------------------------------------------

	@Test
	void get_mapsSoapResponseToRestJson() throws Exception {
		GetCpeIdResponse resp = new GetCpeIdResponse();
		resp.setConfiguration(soapConfig(
				"CPE-1", WifiBandType.BAND_2_4_GHZ, "HomeNet",
				EncryptionType.WPA_2_PSK, "secret123"));
		when(soapClient.getCpeId("CPE-1")).thenReturn(resp);

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
	void get_soapNotFoundFault_returns404() throws Exception {
		// Build the fault first (it stubs its own helper mocks) so we never nest
		// stubbing inside the when(...).thenThrow(...) on soapClient.
		SoapFaultClientException fault = soapFault("CPE not found on platform");
		when(soapClient.getCpeId("UNKNOWN")).thenThrow(fault);

		mockMvc.perform(get("/wifi-parameter/{cpeId}", "UNKNOWN"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CPE_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("CPE not found: UNKNOWN"));
	}

	@Test
	void get_genericSoapFault_returns502() throws Exception {
		SoapFaultClientException fault = soapFault("Internal platform error");
		when(soapClient.getCpeId("CPE-1")).thenThrow(fault);

		mockMvc.perform(get("/wifi-parameter/{cpeId}", "CPE-1"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("SOAP_ERROR"))
				.andExpect(jsonPath("$.message").value("SOAP fault: Internal platform error"));
	}

	@Test
	void get_transportError_returns502() throws Exception {
		// Any non-fault exception (e.g. connection refused) -> 502
		when(soapClient.getCpeId("CPE-1"))
				.thenThrow(new RuntimeException("Connection refused"));

		mockMvc.perform(get("/wifi-parameter/{cpeId}", "CPE-1"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("SOAP_ERROR"))
				.andExpect(jsonPath("$.message")
						.value("SOAP communication error: Connection refused"));
	}

	// ------------------------------------------------------------------
	// PUT /wifi-parameter
	// ------------------------------------------------------------------

	@Test
	void put_validWpa2_returns200_andEchoesConfiguration() throws Exception {
		UpdateCpeIdResponse resp = new UpdateCpeIdResponse();
		resp.setConfiguration(soapConfig(
				"CPE-1", WifiBandType.BAND_5_GHZ, "MyNet",
				EncryptionType.WPA_2_PSK, "strongPass"));
		when(soapClient.updateCpeId(any(WifiConfiguration.class))).thenReturn(resp);

		WifiConfiguration request = restConfig(
				"CPE-1", WifiBandType.BAND_5_GHZ, "MyNet",
				EncryptionType.WPA_2_PSK, "strongPass");

		mockMvc.perform(put("/wifi-parameter")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cpeId").value("CPE-1"))
				.andExpect(jsonPath("$.wifiBandType").value("BAND_5_GHZ"))
				.andExpect(jsonPath("$.ssid").value("MyNet"))
				.andExpect(jsonPath("$.encryptionType").value("WPA_2_PSK"))
				.andExpect(jsonPath("$.password").value("strongPass"));
	}

	@Test
	void put_wpa2WithoutPassword_returns400_validationError() throws Exception {
		// Service-level rule: non-OPEN encryption requires a password.
		WifiConfiguration request = restConfig(
				"CPE-1", WifiBandType.BAND_2_4_GHZ, "MyNet",
				EncryptionType.WPA_2_PSK, null);

		mockMvc.perform(put("/wifi-parameter")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message")
						.value("Password is required for encryption type: WPA_2_PSK"));
	}

	@Test
	void put_openNetwork_clearsPassword_andReturns200() throws Exception {
		// OPEN + a password: service nulls the password before calling SOAP.
		// We echo back whatever configuration the client received.
		when(soapClient.updateCpeId(any(WifiConfiguration.class)))
				.thenAnswer(invocation -> {
					WifiConfiguration received = invocation.getArgument(0);
					UpdateCpeIdResponse resp = new UpdateCpeIdResponse();
					resp.setConfiguration(soapConfig(
							received.getCpeId(),
							received.getWifiBandType(),
							received.getSsid(),
							received.getEncryptionType(),
							received.getPassword()));
					return resp;
				});

		WifiConfiguration request = restConfig(
				"CPE-1", WifiBandType.BAND_2_4_GHZ, "OpenNet",
				EncryptionType.OPEN, "shouldBeRemoved");

		mockMvc.perform(put("/wifi-parameter")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cpeId").value("CPE-1"))
				.andExpect(jsonPath("$.encryptionType").value("OPEN"))
				// password was cleared by the service for OPEN networks;
				// Spring Boot serialises nulls by default so the key is present.
				.andExpect(jsonPath("$.password").value(nullValue()));
	}

	@Test
	void put_beanValidation_missingSsid_returns400() throws Exception {
		WifiConfiguration bad = restConfig(
				"CPE-1", WifiBandType.BAND_2_4_GHZ, "  ",
				EncryptionType.OPEN, null);

		mockMvc.perform(put("/wifi-parameter")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(bad)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void put_soapNotFoundFault_returns404() throws Exception {
		SoapFaultClientException fault = soapFault("cpe not found");
		when(soapClient.updateCpeId(any(WifiConfiguration.class))).thenThrow(fault);

		WifiConfiguration request = restConfig(
				"GHOST", WifiBandType.BAND_2_4_GHZ, "MyNet",
				EncryptionType.OPEN, null);

		mockMvc.perform(put("/wifi-parameter")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CPE_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("CPE not found: GHOST"));
	}

	@Test
	void put_genericSoapFault_returns502() throws Exception {
		SoapFaultClientException fault = soapFault("platform unavailable");
		when(soapClient.updateCpeId(any(WifiConfiguration.class))).thenThrow(fault);

		WifiConfiguration request = restConfig(
				"CPE-1", WifiBandType.BAND_2_4_GHZ, "MyNet",
				EncryptionType.OPEN, null);

		mockMvc.perform(put("/wifi-parameter")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("SOAP_ERROR"))
				.andExpect(jsonPath("$.message").value("SOAP fault: platform unavailable"));
	}

	@Test
	void put_transportError_returns502() throws Exception {
		when(soapClient.updateCpeId(any(WifiConfiguration.class)))
				.thenThrow(new RuntimeException("read timed out"));

		WifiConfiguration request = restConfig(
				"CPE-1", WifiBandType.BAND_2_4_GHZ, "MyNet",
				EncryptionType.OPEN, null);

		mockMvc.perform(put("/wifi-parameter")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("SOAP_ERROR"))
				.andExpect(jsonPath("$.message")
						.value("SOAP communication error: read timed out"));
	}
}

