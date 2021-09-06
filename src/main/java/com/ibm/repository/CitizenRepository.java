package com.ibm.repository;

import com.ibm.entity.Citizen;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface CitizenRepository extends MongoRepository<Citizen, String> {
}
