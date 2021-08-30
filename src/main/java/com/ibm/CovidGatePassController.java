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
    public String requestCovidStatus(@PathVariable String mobileNumber) {
        String json = null;
        String requestId = null;
        ObjectMapper mapper = new ObjectMapper();
        Citizen citizenInfo = citizenRepository.findById(mobileNumber).orElse(null);

        if(citizenInfo ==null){
            requestId = covidGatePassService.extractUserStatus(mobileNumber);

            citizenInfo = citizenRepository.findByRequestId(requestId);

            //citizenInfo = new Citizen(mobileNumber, "NA", "NOT_AVAILABLE");
        }

        try {
            json = mapper.writeValueAsString(citizenInfo);
        }catch (JsonProcessingException e){
            e.printStackTrace();
        }
        return json;
    }

    @PutMapping(path = "/saveCitizen", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Citizen> saveCitizenData(@RequestBody Citizen newCitizen) throws ServerException {
        System.out.println("Saving Citizen data");
        Citizen citizen = citizenRepository.save(newCitizen);

        if (citizen == null) {
            throw new ServerException("Error while saving data");
        }else {
            return new ResponseEntity<>(citizen, HttpStatus.CREATED);
        }
    }

    @GetMapping("/getCitizen")
    public List<Citizen> getCitizenData(){
        System.out.println("Retrieving all citizen information");
        List<Citizen> citizen = citizenRepository.findAll();
        return citizen;
    }

    @GetMapping("/getToken")
    public void getTokenFromASetu(){
        String token = covidGatePassService.extractToken();
        System.out.println(token);
    }

}
