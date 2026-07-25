package com.fachri.produser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProduserApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProduserApplication.class, args);
	}

}
