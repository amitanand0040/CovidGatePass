package com.ibm.dao;

import org.springframework.data.annotation.Id;

import java.util.Date;

public class ASetuToken {
    @Id
    private String token;

    public ASetuToken() {
    }

    public ASetuToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
