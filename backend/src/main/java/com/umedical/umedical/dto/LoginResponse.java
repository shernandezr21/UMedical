package com.umedical.umedical.dto;

public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String role;
    private String fullName;

    public LoginResponse(String token, Long userId, String username, String role, String fullName) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
}