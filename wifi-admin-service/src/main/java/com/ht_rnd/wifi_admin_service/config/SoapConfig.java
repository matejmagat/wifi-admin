package com.ht_rnd.wifi_admin_service.config;

import com.ht_rnd.wifi_admin_service.client.SoapClient;
import com.ht_rnd.wifi_admin_service.client.SoapClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SoapConfig {

    /**
     * Creates and configures the JAXB marshaller used for SOAP request and response payloads.
     *
     * <p>The marshaller is bound to the generated SOAP classes in the
     * {@code local.wifi_admin.platform.v1} package. It also configures a custom namespace
     * prefix mapper so that the target namespace uses the {@code tns} prefix.</p>
     *
     * @return configured JAXB marshaller for SOAP payload serialization and deserialization
     */
    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller m = new Jaxb2Marshaller();
        m.setContextPath("local.wifi_admin.platform.v1");

        Map<String, Object> props = new HashMap<>();
        props.put("org.glassfish.jaxb.namespacePrefixMapper",
                new org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper() {
                    @Override
                    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
                        if ("http://wifi-admin.local/platform/v1".equals(namespaceUri)) {
                            return "tns";
                        }
                        return suggestion;
                    }
                });

        m.setMarshallerProperties(props);
        return m;
    }

    /**
     * Creates and configures the SOAP client used to communicate with the remote SOAP service.
     *
     * <p>The client is initialized with the default endpoint URI, JAXB marshaller,
     * JAXB unmarshaller, and a custom interceptor for SOAP envelope normalization
     * and fault handling.</p>
     *
     * @param marshaller JAXB marshaller bean used for XML binding
     * @return fully configured SOAP client bean
     */
    @Bean
    public SoapClient soapClient(Jaxb2Marshaller marshaller) {
        SoapClient client = new SoapClient();
        client.setDefaultUri("http://localhost:8080/platform");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        client.setInterceptors(new ClientInterceptor[] {
                new SoapClientInterceptor(),
        });
        return client;
    }
}