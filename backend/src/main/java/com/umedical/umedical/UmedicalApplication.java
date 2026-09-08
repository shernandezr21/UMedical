package com.umedical.umedical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UmedicalApplication {

	public static void main(String[] args) {
		SpringApplication.run(UmedicalApplication.class, args);
	}

	// @Bean
    // public CommandLineRunner printPasswordHash() {
    //     return args -> {
    //         String hash = new BCryptPasswordEncoder().encode("password");
    //         System.out.println("=====================================================");
    //         System.out.println("HASH PARA 'password': " + hash);
    //         System.out.println("=====================================================");
    //     };
    // }

}
