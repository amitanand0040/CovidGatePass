package com.ibm;

import com.ibm.model.Citizen;
import com.ibm.repository.CitizenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.rmi.ServerException;
import java.util.List;

@RestController
public class CovidGatePassController {

    @Autowired
    private CitizenRepository repository;

    @PutMapping(path = "/saveCitizen", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Citizen> saveCitizenData(@RequestBody Citizen newCitizen) throws ServerException {
        System.out.println("Saving Citizen data");
        Citizen citizen = repository.save(newCitizen);

        if (citizen == null) {
            throw new ServerException("Error while saving data");
        }else {
            return new ResponseEntity<>(citizen, HttpStatus.CREATED);
        }
    }

    @GetMapping("/getCitizen")
    public List<Citizen> getCitizenData(){
        System.out.println("Retrieving all citizen information");
        List<Citizen> citizen = repository.findAll();
        return citizen;
    }


}
