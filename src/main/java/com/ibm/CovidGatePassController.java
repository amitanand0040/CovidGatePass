package com.ibm;

import com.ibm.entity.Citizen;
import com.ibm.service.CovidGatePassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CovidGatePassController {

    @Autowired
    private CovidGatePassService covidGatePassService;

    @GetMapping("/requestCovidStatus/{mobileNumber}")
    public Citizen getCovidStatus(@PathVariable String mobileNumber) {
        if(!mobileNumber.startsWith("+91")){
            mobileNumber = "+91"+mobileNumber;
        }
        return covidGatePassService.getUserCovidStatus(mobileNumber);
    }

    @GetMapping("/getStatusByReqId/{requestId}")
    public Citizen getStatusByRequestId(@PathVariable String requestId){
        return covidGatePassService.getUserCovidStatusByRequestId(requestId);
    }
}
