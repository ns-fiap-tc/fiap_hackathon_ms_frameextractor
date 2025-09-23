package br.com.fiap.hacka.frameextractorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FrameExtractorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FrameExtractorServiceApplication.class, args);
	}

}