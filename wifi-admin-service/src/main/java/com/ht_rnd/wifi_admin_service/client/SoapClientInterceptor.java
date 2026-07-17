package com.ht_rnd.wifi_admin_service.client;

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

public class SoapClientInterceptor implements ClientInterceptor {

    /**
     * Normalizes the outgoing SOAP envelope before the request is sent.
     *
     * <p>This implementation ensures that the SOAP envelope, header, and body use the
     * {@code soap} namespace prefix instead of any auto-generated prefix. This can be
     * useful when communicating with SOAP servers that are strict about namespace formatting.</p>
     *
     * @param messageContext Spring-WS message context containing the outgoing request
     * @return {@code true} to continue interceptor processing
     * @throws RuntimeException if the SOAP envelope cannot be modified
     */
    @Override
    public boolean handleRequest(MessageContext messageContext) {
        try {
            SaajSoapMessage message = (SaajSoapMessage) messageContext.getRequest();
            SOAPMessage saaj = message.getSaajMessage();
            SOAPEnvelope envelope = saaj.getSOAPPart().getEnvelope();
            SOAPHeader header = envelope.getHeader();
            SOAPBody body = envelope.getBody();

            String oldPrefix = envelope.getPrefix();
            if (oldPrefix != null && !oldPrefix.equals("soap")) {
                envelope.removeNamespaceDeclaration(oldPrefix);
            }

            envelope.addNamespaceDeclaration("soap", envelope.getNamespaceURI());
            envelope.setPrefix("soap");

            if (header != null) {
                header.setPrefix("soap");
            }
            if (body != null) {
                body.setPrefix("soap");
            }

            saaj.saveChanges();
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to normalize SOAP envelope", e);
        }
    }

    /**
     * Inspects the incoming SOAP response for faults.
     *
     * <p>If the SOAP server returns a fault, this method immediately throws a
     * {@code SoapFaultClientException} so the service layer can translate it into
     * an appropriate REST error response.</p>
     *
     * @param messageContext Spring-WS message context containing the SOAP response
     * @return {@code true} if the response does not contain a SOAP fault
     * @throws SoapFaultClientException if the SOAP response contains a fault
     */
    @Override
    public boolean handleResponse(MessageContext messageContext) {
        SoapMessage soapMessage = (SoapMessage) messageContext.getResponse();

        if (soapMessage.hasFault()) {
            throw new SoapFaultClientException(soapMessage);
        }

        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext) {
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) {
    }
}