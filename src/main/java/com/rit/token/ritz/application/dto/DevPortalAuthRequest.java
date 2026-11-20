package com.rit.token.ritz.application.dto;

public class DevPortalAuthRequest {
    private String password;

    public DevPortalAuthRequest() {}

    public DevPortalAuthRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
