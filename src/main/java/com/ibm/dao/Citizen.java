package com.ibm.dao;

import org.springframework.data.annotation.Id;

public class Citizen {

    @Id
    private String mobileNumber;
    private String name;
    private String covidStatus;

    public Citizen() {
    }

    public Citizen(String mobileNumber, String name, String covidStatus) {
        this.mobileNumber = mobileNumber;
        this.name = name;
        this.covidStatus = covidStatus;
    }

    @Override
    public String toString() {
        return "Citizen{" +
                "mobileNumber='" + mobileNumber + '\'' +
                ", name='" + name + '\'' +
                ", covidStatus='" + covidStatus + '\'' +
                '}';
    }

    public String getCovidStatus() {
        return covidStatus;
    }

    public void setCovidStatus(String covidStatus) {
        this.covidStatus = covidStatus;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

}
