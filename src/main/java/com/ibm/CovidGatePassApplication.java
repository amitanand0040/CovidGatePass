package com.ibm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CovidGatePassApplication {

	public static void main(String[] args) {
		SpringApplication.run(CovidGatePassApplication.class, args);
		System.out.println("I am here");
	}
}
