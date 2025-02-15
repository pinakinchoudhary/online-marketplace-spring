package com.onlinemarketplace.walletService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class WalletServiceApplication {

	@Bean
	public RestClient restClient() {
		return RestClient.builder().build();
	}
	public static void main(String[] args) {
		SpringApplication.run(WalletServiceApplication.class, args);
	}

}
