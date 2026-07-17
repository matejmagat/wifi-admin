package com.ht_rnd.wifi_admin_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import local.wifi_admin.platform.v1.EncryptionType;
import local.wifi_admin.platform.v1.WifiBandType;

public class WifiConfiguration {
    @NotBlank
    private String cpeId;
    @NotNull
    private WifiBandType wifiBandType;
    @NotBlank
    private String ssid;
    private EncryptionType encryptionType;
    private String password;


    public WifiBandType getWifiBandType() {
        return wifiBandType;
    }

    public void setWifiBandType(WifiBandType wifiBandType) {
        this.wifiBandType = wifiBandType;
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
}