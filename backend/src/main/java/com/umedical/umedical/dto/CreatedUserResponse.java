package com.umedical.umedical.dto;

public class CreatedUserResponse {

    private UserSummaryResponse user;
    private String username;
    private String temporaryPassword; // null si el admin definio la contrasena

    public CreatedUserResponse(UserSummaryResponse user, String username, String temporaryPassword) {
        this.user = user;
        this.username = username;
        this.temporaryPassword = temporaryPassword;
    }

    public UserSummaryResponse getUser() { return user; }
    public String getUsername() { return username; }
    public String getTemporaryPassword() { return temporaryPassword; }
}