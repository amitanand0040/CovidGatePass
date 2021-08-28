package com.ibm.repository;

import com.ibm.dao.ASetuToken;
import com.ibm.dao.Citizen;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TokenRepository extends MongoRepository<ASetuToken, String> {

}
