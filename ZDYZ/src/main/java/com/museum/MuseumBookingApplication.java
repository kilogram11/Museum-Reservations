package com.museum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class MuseumBookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(MuseumBookingApplication.class, args);
	}

}
