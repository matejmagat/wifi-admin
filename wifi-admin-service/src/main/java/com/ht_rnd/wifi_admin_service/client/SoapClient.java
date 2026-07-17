package com.ht_rnd.wifi_admin_service.client;

import com.ht_rnd.wifi_admin_service.model.WifiConfiguration;
import local.wifi_admin.platform.v1.*;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;

@Component
public class SoapClient extends WebServiceGatewaySupport {

    private static final String SOAP_ENDPOINT = "http://localhost:8080/platform";
    private static final String GET_ACTION    = "http://wifi-admin.local/platform/v1#getCpeID";
    private static final String UPDATE_ACTION = "http://wifi-admin.local/platform/v1#updateCpeId";

    /**
     * Sends a SOAP request to fetch Wi-Fi parameters for the given CPE identifier.
     *
     * <p>This method creates a {@code GetCpeIdRequest}, populates it with the provided
     * CPE ID, and sends it to the configured SOAP endpoint using the corresponding
     * SOAP action.</p>
     *
     * @param cpeId unique identifier of the customer premises equipment
     * @return SOAP response containing the current Wi-Fi configuration
     */
    public GetCpeIdResponse getCpeId(String cpeId) {
        GetCpeIdRequest request = new GetCpeIdRequest();
        request.setCpeId(cpeId);
        return (GetCpeIdResponse) getWebServiceTemplate()
                .marshalSendAndReceive(
                        SOAP_ENDPOINT,
                        request,
                        new SoapActionCallback(GET_ACTION)
                );
    }

    /**
     * Sends a SOAP request to update Wi-Fi parameters on the remote platform.
     *
     * <p>The incoming REST-layer {@code WifiConfiguration} is mapped to the SOAP-generated
     * {@code WifiConfigurationType}. After mapping, the method wraps it in an
     * {@code UpdateCpeIdRequest} and sends it to the SOAP endpoint.</p>
     *
     * @param config REST model containing the Wi-Fi configuration to update
     * @return SOAP response containing the updated Wi-Fi configuration
     */
    public UpdateCpeIdResponse updateCpeId(WifiConfiguration config) {
        WifiConfigurationType soapConfig = new WifiConfigurationType();
        soapConfig.setCpeId(config.getCpeId());
        soapConfig.setWifiBand(WifiBandType.fromValue(config.getWifiBandType().name()));
        soapConfig.setSsid(config.getSsid());

        if (config.getEncryptionType() != null) {
            soapConfig.setEncryptionType(
                    EncryptionType.fromValue(config.getEncryptionType().value())
            );
        }

        soapConfig.setPassword(config.getPassword());

        UpdateCpeIdRequest request = new UpdateCpeIdRequest();
        request.setConfiguration(soapConfig);

        return (UpdateCpeIdResponse) getWebServiceTemplate()
                .marshalSendAndReceive(
                        SOAP_ENDPOINT,
                        request,
                        new SoapActionCallback(UPDATE_ACTION)
                );
    }
}


