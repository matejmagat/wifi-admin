package com.ht_rnd.wifi_admin_service.repository;

import com.ht_rnd.wifi_admin_service.entity.WifiConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WifiConfigurationRepository extends JpaRepository<WifiConfigurationEntity, Long> {
    Optional<WifiConfigurationEntity> findByCpeId(String cpeId);
}