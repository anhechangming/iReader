package com.library.ireaderbackend.entity;

public class LoginResponse {
    private String token;
    private User user;  // 这里是你自己的 User 实体类

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LoginResponse(User user, String token) {
        this.user = user;
        this.token = token;
    }

    public LoginResponse() {
    }
}