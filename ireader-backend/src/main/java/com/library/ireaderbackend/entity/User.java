package com.library.ireaderbackend.entity;


import lombok.Data;

import java.time.LocalDateTime;


public class User {
    private Long id;
    private String phone;
    private String password;
    private String nickname;
    private LocalDateTime create_Time;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public LocalDateTime getCreate_Time() {
        return create_Time;
    }

    public void setCreate_Time(LocalDateTime create_Time) {
        this.create_Time = create_Time;
    }
}