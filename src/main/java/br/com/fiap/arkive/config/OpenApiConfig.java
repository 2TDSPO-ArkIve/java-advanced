package br.com.fiap.arkive.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI arkiveOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Arkive API")
						.description("API REST para jornada contínua de saúde do pet")
						.version("0.0.1-SNAPSHOT"));
	}

}
