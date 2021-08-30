package com.ibm.repository;

import com.ibm.dao.Citizen;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface CitizenRepository extends MongoRepository<Citizen, String> {

    public Citizen findByRequestId(String requestId);

    /*public Citizen findByName (String name);

    public Citizen updateCitizen (String mobileNumber);*/
}
