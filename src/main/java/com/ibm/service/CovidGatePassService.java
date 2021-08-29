package com.ibm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.dao.ASetuToken;
import com.ibm.dao.RequestTracker;
import com.ibm.model.UserCredentials;
import com.ibm.model.UserDetails;
import com.ibm.repository.CitizenRepository;
import com.ibm.repository.RequestTrackerRepository;
import com.ibm.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.rmi.ServerException;
import java.time.Instant;
import java.util.*;

@Component
public class CovidGatePassService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private RequestTrackerRepository trackerRepository;

    public void extractUserStatus(String mobileNumber) {
        // Extract token to invoke Aarogya Setu API call
        String token = this.extractToken();
        System.out.println(token);

        RestTemplate restTemplate = new RestTemplate();
        RequestTracker requestTracker = new RequestTracker();

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

            RequestTracker requestIdTracker = new ObjectMapper().readValue(response.getBody().toString(), RequestTracker.class);
            String reqID = requestIdTracker.getRequestId();


            System.out.println(response.getBody().toString());

            requestTracker.setMobileNumber(mobileNumber);
            requestTracker.setTrace_id(uniqueID);
            requestTracker.setRequestId(reqID);
            requestIdTracker.setDate(Instant.now());
            requestIdTracker.setRequest_status("PENDING");

            trackerRepository.save(requestIdTracker);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String extractToken()  {
        String token = null;
        boolean isTokenValid = false;
        List<ASetuToken> aSetuTokenList = tokenRepository.findAll();

        if(!aSetuTokenList.isEmpty()){
            String searchToken = aSetuTokenList.stream().findFirst().toString();
            if(validateToken(searchToken)){
                token = searchToken;
                isTokenValid = true;
            }
        }
        // If token is not valid/expired, invoke Aarogya setu API to generate new token
        if(!isTokenValid){
            ResponseEntity<String> response = this.fetchTokenFromASetu();
            if(response != null) {
                try {
                    ASetuToken aSetuToken = new ObjectMapper().readValue(response.getBody().toString(), ASetuToken.class);
                    token = aSetuToken.getToken();

                    // Storing token in Database for further use
                    ASetuToken tokenSave = tokenRepository.save(aSetuToken);
                    if (tokenSave == null) {
                        throw new ServerException("Error while saving token");
                    }

                }catch (JsonProcessingException | ServerException e){
                    e.printStackTrace();
                }
            }
        }
        return token;
    }

    // TODO validate JWT token for expiry
    private boolean validateToken(String token) {
        String temp = "eyJraWQiOiJUcFhOUEFKZytpQWQ2Z0Jkc1dwYk9US1Z6d3FHUGdHWFZJYzVcL3MxZmp4UT0iLCJhbGciOiJSUzI1NiJ9.eyJjdXN0b206c2VjcmV0S2V5Ijoic3NoLXJzYSBBQUFBQjNOemFDMXljMkVBQUFBREFRQUJBQUFCZ1FEYkF4NDJaV0JURTNwRytBN01sWmFiT2VCdWs3TWZ5ZXhSaHpTYVVRa1Z3ekNNbHdwMEo4dlQzdnR2a2lxTG43TFRtbjc0WDJCZUgxUXJkZVFyN3hSd3hhSklOZmZFZnk4cXdlN0hka0RHTkVwckJKV2FFRXA2cmxWM3hIa1JzcTVGNmVnSHZFOEozM2l4eHZVcUltcDFJMFZheG9EMklQXC9YWCtFeDlrZTZ5Zkg5M2M0OHM1YUh5b3VRaWRJMHErSXBiS0oraSswRHE5VGtSOHRoRjJSaVB0aEJrdjRYNWhXcXBMN2RCOUhvTlJpb1dcLzZhdTJxeVVvdk9qbUtTRDlIWkhMVmdweTI0V0QzUHBVQVlTQXpvNFwvU3ZVWE1nQlhNYW4rNXNmQmd2ZzJTMXFRREVxVjRiNG9oeUw4b2ZlR2FoXC9QamxaQ3haK0czV0xsUmkzSGt5dkpYTHloMzVsNFQ4Z2tUcVJIbTZMalJIVmlaeHRrV0FVVVNyTWYzVURuK2pzR0srMUNjYW1iY2RPQjFtOVY0REdHKzRBSk9WeXo1VWVZMEhWbnBiU05YbUVkeFRZcjhQcEtuSXFvU0dzTWVoaDFxUG5ZMGQ2ajRMdUIwb1Bva2tpN3pvQUFsVVwvQmR2TVRwd2xaRCsxTTQ5V01kKzdmMVkyenhJXC9jczFjbE09IEFNQU5BTkQ5QE5CU1AwOTcyNzctVFAiLCJzdWIiOiJjYjJhYWRjMS1mZGI3LTRjMDUtYjAxYy1iMGY2NzZlZjhiOGMiLCJjb2duaXRvOmdyb3VwcyI6WyJkZXYtcG9ydGFsLVJlZ2lzdGVyZWRHcm91cCJdLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiY29nbml0bzpwcmVmZXJyZWRfcm9sZSI6ImFybjphd3M6aWFtOjo3MjY5NDEzMDc4MDc6cm9sZVwvZGV2LXBvcnRhbC1Db2duaXRvUmVnaXN0ZXJlZFJvbGUtMTE0SFRHMElZT0JCTiIsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC5hcC1zb3V0aC0xLmFtYXpvbmF3cy5jb21cL2FwLXNvdXRoLTFfSmd6ZlRZVHRwIiwicGhvbmVfbnVtYmVyX3ZlcmlmaWVkIjp0cnVlLCJjb2duaXRvOnVzZXJuYW1lIjoiY2IyYWFkYzEtZmRiNy00YzA1LWIwMWMtYjBmNjc2ZWY4YjhjIiwiY3VzdG9tOkNhbGxiYWNrVVJMIjoiaHR0cDpcL1wvIDE3Mi4yMC4xMjguMTo4MDgwXC91cGRhdGVVc2VyRGV0YWlscyIsImN1c3RvbTp0bmMiOiJ0cnVlIiwiY29nbml0bzpyb2xlcyI6WyJhcm46YXdzOmlhbTo6NzI2OTQxMzA3ODA3OnJvbGVcL2Rldi1wb3J0YWwtQ29nbml0b1JlZ2lzdGVyZWRSb2xlLTExNEhURzBJWU9CQk4iXSwiYXVkIjoiN3BoNml0ZmdwOGU3bXNldjVyazNlbzVrNW8iLCJldmVudF9pZCI6Ijg4ZjZhNTFmLTZhNTEtNDcxNC1iODZmLTcyYjNhYzdkYmUwYyIsInRva2VuX3VzZSI6ImlkIiwiYXV0aF90aW1lIjoxNjMwMTYxNDEwLCJwaG9uZV9udW1iZXIiOiIrOTE4OTY5NTMwMDQyIiwiY3VzdG9tOmNvbXBhbnlOYW1lIjoiSUJNIiwiZXhwIjoxNjMwMTY1MDA5LCJpYXQiOjE2MzAxNjE0MTAsImVtYWlsIjoiYW1pdC5hbmFuZDAwNEBnbWFpbC5jb20ifQ.KuwvTqNTQUw3_ep-4LXDY4BlOmA85rozpu0Mby9Or1hbe5gQ_CHGdRKp1s7XU3d90_EygHhh2JzaFhI0L6OW-0veNhtqaHv1wNpiNaF9r3cTcguJShC8iDZz8WEfRGkUSw41gOCBo39R40rI-EeYZaggJol3bYI1s5Lh8e1CSr_KOF_bZY-y4hL-EwaqK1xmIQwb8Qi_MVU5Ek8ypMd9PvHHlrs7ZPHnyJlNHHLCVFQm3-X233Pz7uPnXLDdslxeVk0Tl5HAFcc3kwWs4qu4bglTn0HIlxOd32MjdODKjdVQwckCpiOuT8vlId69walXsji-reRhEVMgXUNbze1Xsg";

        Base64.Decoder decoder = Base64.getDecoder();
        String[] splitToken = token.split("\\.");
        String payload = new String(decoder.decode(splitToken[1]));
        //Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        return false;
    }

    private ResponseEntity<String> fetchTokenFromASetu(){
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

}
