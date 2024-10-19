package com.intern.coursemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication
public class CoursemateApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoursemateApplication.class, args);
	}
}

