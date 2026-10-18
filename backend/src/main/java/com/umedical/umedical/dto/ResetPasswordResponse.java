package com.umedical.umedical.dto;

public class ResetPasswordResponse {

    private String temporaryPassword;

    public ResetPasswordResponse(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public String getTemporaryPassword() { return temporaryPassword; }
}