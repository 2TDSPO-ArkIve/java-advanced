package br.com.fiap.arkive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class ArkiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArkiveApplication.class, args);
	}

}
