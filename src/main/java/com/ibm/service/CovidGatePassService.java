package com.ibm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.entity.ASetuToken;
import com.ibm.entity.Citizen;
import com.ibm.entity.RequestTracker;
import com.ibm.model.UserCredentials;
import com.ibm.model.UserDetails;
import com.ibm.repository.CitizenRepository;
import com.ibm.repository.RequestTrackerRepository;
import com.ibm.repository.TokenRepository;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

import static com.ibm.Constant.AppConstant.*;

@Component
public class CovidGatePassService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private RequestTrackerRepository trackerRepository;

    @Autowired
    MongoOperations mongoOperations;

    public Citizen getUserCovidStatus(String mobileNumber) {
        // Search mobile number in database
        Citizen citizenInfo = citizenRepository.findById(mobileNumber).orElse(null);
        // If not found, invoke API
        if(citizenInfo ==null){
            // Invoke service to extract details from ASetu
            System.out.println("Citizen information NOT available in db");
            citizenInfo = this.getUserStatusFromASetu(mobileNumber);
        }else{
            System.out.println("Fetching Citizen information from db");
        }
        return citizenInfo;
    }

    public Citizen getUserCovidStatusByRequestId(String requestId) {
        // Updated Request Tracker DB
        System.out.println("Extracting user status by requestId from API");
        this.updateRequestTrackerDB(requestId);
        // Updated Citizen DB
        return this.updateCitizenDB(requestId);
    }


    private Citizen getUserStatusFromASetu(String mobileNumber) {
        Citizen citizen = new Citizen();
        RestTemplate restTemplate = new RestTemplate();
        RequestTracker requestTracker = new RequestTracker();
        ResponseEntity<String> response;
        String uniqueID = "IBM_"+UUID.randomUUID();

        // Extract token to invoke Aarogya Setu API call
        String token = this.extractToken();

        UserDetails userDetails = new UserDetails();
        userDetails.setPhone_number(mobileNumber);
        userDetails.setTrace_id(uniqueID);
        userDetails.setReason("Get covid status");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", decoder(AAROGYA_SETU_CRED3));
        headers.set("Authorization", token);

        try {
            // Temporary arrangement until API full access is approved
            if(mobileNumber.equals("+918969530042")) {
                HttpEntity<UserDetails> entity = new HttpEntity<>(userDetails, headers);
                System.out.println("Invoking AAROGYA SETU API for fetching userstatus");
                response = restTemplate.exchange(AAROGYA_SETU_USERSTATUS_URI, HttpMethod.POST, entity, String.class);

                RequestTracker requestIdTracker = new ObjectMapper().readValue(response.getBody(), RequestTracker.class);
                String requestId = requestIdTracker.getRequestId();

                // Update RequestTracker repository
                if (requestId != null) {
                    // Save request details in database
                    requestTracker.setTrace_id(uniqueID);
                    requestTracker.setRequestId(requestId);
                    requestTracker.setRequest_status("PENDING");
                    trackerRepository.save(requestTracker);

                    // Save citizen information in database
                    citizen.setMobileNumber(mobileNumber);
                    citizen.setCovidStatus("PENDING");
                    citizen.setRequestId(requestId);
                    citizenRepository.save(citizen);
                }
            }else{
                // Temporary arrangement until API full access is approved
                System.out.println("Fetching Citizen status from API");
                String requestIDTemp = UUID.randomUUID().toString();

                requestTracker.setTrace_id(uniqueID);
                requestTracker.setRequestId(requestIDTemp);
                requestTracker.setRequest_status("PENDING");
                trackerRepository.save(requestTracker);

                citizen.setMobileNumber(mobileNumber);
                citizen.setCovidStatus("PENDING");
                citizen.setRequestId(requestIDTemp);
                citizenRepository.save(citizen);
            }
            System.out.println("Successfully updated Citizen db..!!");
        } catch (Exception e){
            e.printStackTrace();
        }
        return citizen;
    }

    private String extractToken() {
        String token = null;
        boolean isTokenValid = false;
        List<ASetuToken> aSetuTokenList = tokenRepository.findAll();
        if(!aSetuTokenList.isEmpty()){
            System.out.println("Token available in db");
            String recentToken = aSetuTokenList.get(aSetuTokenList.size()-1).getToken();
            if(validateToken(recentToken)){
                token = recentToken;
                isTokenValid = true;

            }
        }
        // If token is not valid/expired, invoke Aarogya setu API to generate new token
        if(!isTokenValid){
            System.out.println("Expired Token in db");
            token = this.getTokenFromASetu();
        }
        return token;
    }

    // TODO validate JWT token for expiry
    private boolean validateToken(String token) {
        JWSObject jwsObject;
        JWTClaimsSet claims;
        long tokenValid = 0;
        try {
            jwsObject = JWSObject.parse(token);
            claims =  JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());
            Date claimExpDate = claims.getExpirationTime();
            long tokenExpDate = claimExpDate.toInstant().toEpochMilli();
            long currDate = Instant.now().toEpochMilli();
            tokenValid = tokenExpDate - currDate;
        } catch (java.text.ParseException e) {
            // Invalid JWS object encoding
            System.out.println("Error while paring token");
        }

        if(tokenValid > 0 ){
            System.out.println("Token is still VALID (expiry 1hr)");
            return true;
        }else{
            return false;
        }
    }

    private String getTokenFromASetu(){
        ResponseEntity<String> response = null;
        UserCredentials userCredentials = new UserCredentials(decoder(AAROGYA_SETU_CRED1), decoder(AAROGYA_SETU_CRED2));
        RestTemplate restTemplate = new RestTemplate();
        ASetuToken aSetuToken = null;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", decoder(AAROGYA_SETU_CRED3));

        HttpEntity<UserCredentials> entity = new HttpEntity<>(userCredentials, headers);
        System.out.println("Invoking ASETU API for fetching Token for further API invocations");
        response = restTemplate.exchange(AAROGYA_SETU_GETTOKEN_URI, HttpMethod.POST,entity, String.class);

        if(response.getBody() != null) {
            try {
                aSetuToken = new ObjectMapper().readValue(response.getBody(), ASetuToken.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            // Storing token in Database for further use
            tokenRepository.save(aSetuToken);
        }
        return aSetuToken.getToken();
    }


    // Invoke once Aarogya setu full request is approved
    public Citizen getUserStatusByRequstIdFromASetu(String requestId) {
        // Extract token to invoke Aarogya Setu API call
        //requestId = "5bcdcda5-2601-4a91-a62c-c2baed0dd334";
        String token = this.extractToken();
        System.out.println(token);

        Citizen citizen = new Citizen();
        ResponseEntity<RequestTracker> response = null;
        RestTemplate restTemplate = new RestTemplate();
        //String uniqueID = UUID.randomUUID().toString().toUpperCase().replace("-", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", decoder(AAROGYA_SETU_CRED3));
        headers.set("Authorization", token);

        try{
            RequestTracker requestTracker = new RequestTracker();
            requestTracker.setRequestId(requestId);
            HttpEntity<RequestTracker> entity = new HttpEntity<>(requestTracker, headers);
            System.out.println("Invoking AAROGYA SETU API userstatusbyreqid");
            response = restTemplate.exchange(AAROGYA_SETU_STATUS_BY_REQUESTID_URI, HttpMethod.POST, entity, RequestTracker.class);
        }catch (HttpServerErrorException e){
        }
        if(response.getStatusCode().equals(HttpStatus.OK)){
            // TODO Incase of success
            this.updateRequestTrackerDB(requestId);
            citizen = this.updateCitizenDB(requestId);
        }
        System.out.println("Successfully updated Citizen information in db..!!");
        return citizen;
    }

    private Citizen updateCitizenDB(String requestId) {
        Citizen citizen;
        Query queryCitz = new Query();
        queryCitz.addCriteria(Criteria.where("requestId").is(requestId));


        citizen = mongoOperations.findOne(queryCitz,Citizen.class);

        if(citizen.getMobileNumber().contains("8969530041")){
            citizen.setName("Shikha Anand");
            citizen.setCovidStatus("FULLY VACCINATED");
        } else if(citizen.getMobileNumber().contains("8969530040")){
            citizen.setName("Ayansh Anand");
            citizen.setCovidStatus("NOT VACCINATED");
        } else{
            citizen.setName("Amit Anand");
            citizen.setCovidStatus("PARTIALLY VACCINATED");
        }

        mongoOperations.save(citizen);
        System.out.println("Successfully updated Citizen information in db..!!");
        return citizen;
    }

    private void updateRequestTrackerDB(String reqId){
        String as_Status = "eyJhbGciOiJIUzI1NiJ9.eyJhc19zdGF0dXMiOnsiY29sb3JfY29kZSI6IiMzQUE4NEMiLCJtZXNzYWdlIjoiU2FtcGxlIHVzZXIgKCs5MTg5Njk1MzAwNDIpIGlzIHNhZ" +
                "mUiLCJtb2JpbGVfbm8iOiIrOTE4OTY5NTMwMDQyIiwibmFtZSI6IkFtaXQgQW5hbmQiLCJzdGF0dXNfY29kZSI6MzAwfX0.AYVj3tynLeob2ZqFxOkQZ4D5vWXsUzFjxjIfHYGYoDU";

        Query queryReqTrk = new Query();
        queryReqTrk.addCriteria(Criteria.where("requestId").is(reqId));

        RequestTracker requestTracker = mongoOperations.findOne(queryReqTrk, RequestTracker.class);
        requestTracker.setRequest_status("APPROVED");
        requestTracker.setAs_status("Amit Anand");
        requestTracker.setAs_status(as_Status);
        mongoOperations.save(requestTracker);
    }

    private String decoder(String input){
        String decoded = new String(Base64.getDecoder().decode(input));
        return decoded;
    }
}
