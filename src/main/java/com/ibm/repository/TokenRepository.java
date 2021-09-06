package com.ibm.repository;

import com.ibm.entity.ASetuToken;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TokenRepository extends MongoRepository<ASetuToken, String> {

}
