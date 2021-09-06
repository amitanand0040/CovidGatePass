package com.ibm.repository;

import com.ibm.entity.RequestTracker;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RequestTrackerRepository extends MongoRepository<RequestTracker, String> {

}
