package br.com.fiap.arkive.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ExpoPushProperties.class)
public class ExpoPushConfig {

	@Bean
	@Qualifier("expoPushRestClient")
	RestClient expoPushRestClient(ExpoPushProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
		requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());
		return RestClient.builder().requestFactory(requestFactory).build();
	}
}

