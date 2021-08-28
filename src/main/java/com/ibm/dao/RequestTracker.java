package com.ibm.dao;

import org.springframework.data.annotation.Id;

import java.util.UUID;

public class RequestTracker {
    @Id
    private String mobileNumber;
    private String request_id;
    private String trace_id;
    private String request_status;
    private String as_status;




}
