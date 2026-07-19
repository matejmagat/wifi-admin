package com.ht_rnd.wifi_admin_service.service;

import com.ht_rnd.wifi_admin_service.client.SoapClient;
import com.ht_rnd.wifi_admin_service.entity.WifiConfigurationEntity;
import com.ht_rnd.wifi_admin_service.mapper.WifiConfigurationMapper;
import com.ht_rnd.wifi_admin_service.repository.WifiConfigurationRepository;
import local.wifi_admin.platform.v1.GetCpeIdResponse;
import local.wifi_admin.platform.v1.WifiConfigurationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.util.List;

@Service
@ConditionalOnProperty(name = "wifi.sync.enabled", havingValue = "true", matchIfMissing = true)
public class WifiSyncService {

    private static final Logger log = LoggerFactory.getLogger(WifiSyncService.class);

    private final WifiConfigurationRepository wifiConfigurationRepository;
    private final WifiConfigurationMapper wifiConfigurationMapper;
    private final SoapClient soapClient;

    public WifiSyncService(
            WifiConfigurationRepository wifiConfigurationRepository,
            WifiConfigurationMapper wifiConfigurationMapper,
            SoapClient soapClient
    ) {
        this.wifiConfigurationRepository = wifiConfigurationRepository;
        this.wifiConfigurationMapper = wifiConfigurationMapper;
        this.soapClient = soapClient;
    }

    @Scheduled(
            initialDelayString = "${wifi.sync.initial-delay-ms:10000}",
            fixedDelayString = "${wifi.sync.fixed-delay-ms:60000}"
    )
    public void syncAllKnownWifiConfigurations() {
        List<WifiConfigurationEntity> entities = wifiConfigurationRepository.findAll();

        if (entities.isEmpty()) {
            log.debug("Wi-Fi sync skipped: no local configurations to refresh.");
            return;
        }

        log.info("Starting Wi-Fi sync for {} configuration(s).", entities.size());

        int successCount = 0;
        int failureCount = 0;

        for (WifiConfigurationEntity entity : entities) {
            String cpeId = entity.getCpeId();

            try {
                GetCpeIdResponse response = soapClient.getCpeId(cpeId);
                WifiConfigurationType soapConfig = response.getConfiguration();

                wifiConfigurationMapper.updateEntityFromSoap(soapConfig, entity);
                wifiConfigurationRepository.save(entity);

                successCount++;
                log.debug("Synced configuration for cpeId={}", cpeId);

            } catch (SoapFaultClientException e) {
                failureCount++;
                log.warn("SOAP fault while syncing cpeId={}: {}", cpeId, e.getFaultStringOrReason());
            } catch (Exception e) {
                failureCount++;
                log.warn("Failed to sync cpeId={}: {}", cpeId, e.getMessage());
            }
        }

        log.info("Wi-Fi sync finished. success={}, failed={}", successCount, failureCount);
    }
}