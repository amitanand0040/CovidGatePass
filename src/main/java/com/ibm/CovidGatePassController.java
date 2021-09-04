package com.ibm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.dao.Citizen;
import com.ibm.model.UserDetails;
import com.ibm.repository.CitizenRepository;
import com.ibm.service.CovidGatePassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import java.rmi.ServerException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class CovidGatePassController {

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private CovidGatePassService covidGatePassService;

    @GetMapping("/requestCovidStatus/{mobileNumber}")
    public Citizen getCovidStatus(@PathVariable String mobileNumber) {
        if(!mobileNumber.startsWith("+91")){
            mobileNumber = "+91"+mobileNumber;
        }
        Citizen citizenInfo = covidGatePassService.extractUserStatus(mobileNumber);
        return citizenInfo;
    }

    @GetMapping("/getStatusByReqId/{requestId}")
    public Citizen getStatusByRequestId(@PathVariable String requestId){
        Citizen citizen = covidGatePassService.getUserStatusByRequstId(requestId);
        return citizen;
    }
}
