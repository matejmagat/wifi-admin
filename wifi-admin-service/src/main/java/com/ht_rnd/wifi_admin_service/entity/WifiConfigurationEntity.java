package com.ht_rnd.wifi_admin_service.entity;

import jakarta.persistence.*;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.WifiBandType;

import java.time.LocalDateTime;


/**
 * Persistence entity representing a Wi-Fi configuration stored in the database.
 * Each record is associated with a single CPE identifier and contains the
 * Wi-Fi settings most recently retrieved from or synchronized with the
 * external SOAP platform.
 */
@Entity
@Table(name = "wifi_configuration")
public class WifiConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cpe_id", nullable = false, unique = true)
    private String cpeId;

    @Column(nullable = false)
    private String ssid;

    @Enumerated(EnumType.STRING)
    @Column(name = "encryption_type")
    private EncryptionType encryptionType;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "wifi_band_type", nullable = false)
    private WifiBandType wifiBandType;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    public Long getId() {
        return id;
    }

    public String getCpeId() {
        return cpeId;
    }

    public void setCpeId(String cpeId) {
        this.cpeId = cpeId;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public EncryptionType getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(EncryptionType encryptionType) {
        this.encryptionType = encryptionType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public WifiBandType getWifiBandType() {
        return wifiBandType;
    }

    public void setWifiBandType(WifiBandType wifiBandType) {
        this.wifiBandType = wifiBandType;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}