package com.ibm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.model.ASetuToken;
import com.ibm.model.UserCredentials;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Component
public class CovidGatePassService {

    public static String getToken(){
        String resourceUrl = "https://api.aarogyasetu.gov.in/token";
        String token = null;
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername("amit.anand004@gmail.com");
        userCredentials.setPassword("ShikhaBharti@143");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", "gvhed11S9z53uDfZvsEni4ScuJx9yu8T9dd3BjL1");

        try {
            HttpEntity<UserCredentials> entity = new HttpEntity<>(userCredentials, headers);
            ResponseEntity<String> response = restTemplate.exchange(resourceUrl, HttpMethod.POST,entity, String.class);
            ASetuToken aSetuToken = new ObjectMapper().readValue(response.getBody().toString(), ASetuToken.class);

            token = aSetuToken.getToken();

        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            return token;
        }
    }
}
