package com.museum.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "booking.scheduler.enabled=false")
class MuseumBookingApplicationTests {

	@Test
	void contextLoads() {
	}

}
