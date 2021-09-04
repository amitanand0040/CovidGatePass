package com.ibm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.dao.ASetuToken;
import com.ibm.dao.Citizen;
import com.ibm.dao.RequestTracker;
import com.ibm.model.UserCredentials;
import com.ibm.model.UserDetails;
import com.ibm.repository.CitizenRepository;
import com.ibm.repository.RequestTrackerRepository;
import com.ibm.repository.TokenRepository;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.rmi.ServerException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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

    public Citizen extractUserStatus(String mobileNumber) {
        Citizen citizen = new Citizen();
        RestTemplate restTemplate = new RestTemplate();
        RequestTracker requestTracker = new RequestTracker();

        // Extract token to invoke Aarogya Setu API call
        String token = null;
        token = this.extractToken();

        String resourceUrl = "https://api.aarogyasetu.gov.in/userstatus";
        String uniqueID = UUID.randomUUID().toString().toUpperCase().replace("-", "");

        UserDetails userDetails = new UserDetails();
        userDetails.setPhone_number(mobileNumber);
        userDetails.setTrace_id(uniqueID);
        userDetails.setReason("Get covid status");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", "gvhed11S9z53uDfZvsEni4ScuJx9yu8T9dd3BjL1");
        headers.set("Authorization", token);

        try {
            HttpEntity<UserDetails> entity = new HttpEntity<>(userDetails, headers);
            ResponseEntity<String> response = restTemplate.exchange(resourceUrl, HttpMethod.POST, entity, String.class);

            RequestTracker requestIdTracker = new ObjectMapper().readValue(response.getBody(), RequestTracker.class);
            String requestId = requestIdTracker.getRequestId();

            // Update RequestTracker repository
            if(requestId !=null) {
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


            // Update Citizen repository with request ID
           /* Query query = new Query();
            query.addCriteria(Criteria.where("mobileNumber").is(mobileNumber));
            //query.fields().include("mobileNumber");

            Citizen citizen = mongoOperations.findOne(query, Citizen.class);
            mongoOperations.save(citizen);*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        return citizen;
    }

    public String extractToken() {
        String token = null;
        boolean isTokenValid = false;
        List<ASetuToken> aSetuTokenList = tokenRepository.findAll();
        if(!aSetuTokenList.isEmpty()){

            String searchToken= aSetuTokenList.get(aSetuTokenList.size()-1).getToken();
            System.out.println(searchToken);
            if(validateToken(searchToken)){
                token = searchToken;
                isTokenValid = true;
                System.out.println("Extracted token from database");
            }
        }
        // If token is not valid/expired, invoke Aarogya setu API to generate new token
        if(!isTokenValid){
            ResponseEntity<String> response = this.getTokenFromASetu();
            if(response.getBody() != null) {
                ASetuToken aSetuToken = null;
                try {
                    aSetuToken = new ObjectMapper().readValue(response.getBody(), ASetuToken.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                token = aSetuToken.getToken();

                // Storing token in Database for further use
                tokenRepository.save(aSetuToken);
            }
        }
        return token;
    }

    // TODO validate JWT token for expiry
    public boolean validateToken(String token) {
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
            return true;
        }else{
            return false;
        }
    }

    private ResponseEntity<String> getTokenFromASetu(){
        String resourceUrl = "https://api.aarogyasetu.gov.in/token";
        ResponseEntity<String> response = null;
        UserCredentials userCredentials = new UserCredentials("amit.anand004@gmail.com", "ShikhaBharti@143");
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", "gvhed11S9z53uDfZvsEni4ScuJx9yu8T9dd3BjL1");

        HttpEntity<UserCredentials> entity = new HttpEntity<>(userCredentials, headers);
        response = restTemplate.exchange(resourceUrl, HttpMethod.POST,entity, String.class);
        return response;
    }

    public Citizen getUserStatusByRequstId(String requestId){
        // Extract token to invoke Aarogya Setu API call
        requestId = "5bcdcda5-2601-4a91-a62c-c2baed0dd334";
        String token = this.extractToken();
        System.out.println(token);

        Citizen citizen = new Citizen();

        RestTemplate restTemplate = new RestTemplate();

        String resourceUrl = "https://api.aarogyasetu.gov.in/userstatusbyreqid";
        //String uniqueID = UUID.randomUUID().toString().toUpperCase().replace("-", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", "gvhed11S9z53uDfZvsEni4ScuJx9yu8T9dd3BjL1");
        headers.set("Authorization", token);

        try {
            HttpEntity<String> entity = new HttpEntity<>(requestId, headers);
            ResponseEntity<RequestTracker> response = restTemplate.exchange(resourceUrl, HttpMethod.POST, entity, RequestTracker.class);

            if(response.getStatusCode().equals(HttpStatus.OK)){
                RequestTracker status =  new ObjectMapper().readValue(response.getBody().toString(), RequestTracker.class);
                status.getAs_status();

                String temp = "eyJhbGciOiJIUzI1NiJ9.eyJhc19zdGF0dXMiOnsiY29sb3JfY29kZSI6IiMzQUE4NEMiLCJtZXNzYWdlIjoiU2FtcGxlIHVzZXIgKCs5MTk5eHh4eHh4eHgpIGlzIHNhZmUiLCJtb2JpbGVfbm8iOiIrOTE5OXh4eHh4eHh4IiwibmFtZSI6IlNhbXBsZSBVc2VyIiwic3RhdHVzX2NvZGUiOjMwMH19.AYVj3tynLeob2ZqFxOkQZ4D5vWXsUzFjxjIfHYGYoDU";

               /* Citizen citizen = new Citizen();
                citizen.setName(status.getName());*/
            }else if(response.getStatusCode().equals(HttpStatus.BAD_REQUEST) || response.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)){
                // TODO handle error message
                throw new Exception();
            }



           /* // Update Citizen repository with request ID
            Query query = new Query();
            query.addCriteria(Criteria.where("mobileNumber").is(mobileNumber));
            //query.fields().include("mobileNumber");

            Citizen citizen = mongoOperations.findOne(query, Citizen.class);
            citizen.setMobileNumber(mobileNumber);
            citizen.setCovidStatus("PENDING");
            citizen.setRequestId(requestId);
            mongoOperations.save(citizen);*/

        } catch (Exception e) {
            e.printStackTrace();
        }
        return citizen;
    }
}
