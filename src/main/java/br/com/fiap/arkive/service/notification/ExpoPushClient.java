package br.com.fiap.arkive.service.notification;

import br.com.fiap.arkive.config.ExpoPushProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class ExpoPushClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExpoPushClient.class);
	private static final String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

	private final RestClient restClient;
	private final ExpoPushProperties properties;

	public ExpoPushClient(@Qualifier("expoPushRestClient") RestClient restClient,
			ExpoPushProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	public ExpoPushResult enviar(ExpoPushMessage message) {
		if (!properties.isEnabled()) {
			return ExpoPushResult.SKIPPED;
		}
		try {
			ExpoPushResponse response = restClient.post()
					.uri(properties.getUrl())
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.headers(headers -> adicionarAutorizacao(headers, properties.getAccessToken()))
					.body(List.of(message))
					.retrieve()
					.body(ExpoPushResponse.class);
			return interpretar(response);
		} catch (ResourceAccessException ex) {
			LOGGER.warn("Expo Push indisponivel por timeout ou falha de conexao.");
			return ExpoPushResult.FAILED;
		} catch (RestClientResponseException ex) {
			LOGGER.warn("Expo Push respondeu com HTTP {}.", ex.getStatusCode().value());
			return ExpoPushResult.FAILED;
		} catch (RestClientException ex) {
			LOGGER.warn("Expo Push retornou resposta invalida.");
			return ExpoPushResult.FAILED;
		}
	}

	private ExpoPushResult interpretar(ExpoPushResponse response) {
		if (response == null || response.data() == null || response.data().size() != 1) {
			LOGGER.warn("Expo Push retornou quantidade inesperada de tickets.");
			return ExpoPushResult.FAILED;
		}
		ExpoPushTicket ticket = response.data().get(0);
		if ("ok".equalsIgnoreCase(ticket.status())) {
			return ExpoPushResult.SUCCESS;
		}
		Object error = ticket.details() == null ? null : ticket.details().get("error");
		if (DEVICE_NOT_REGISTERED.equals(error)) {
			return ExpoPushResult.DEVICE_NOT_REGISTERED;
		}
		LOGGER.warn("Expo Push rejeitou uma notificacao com erro de ticket sanitizado: {}.", error);
		return ExpoPushResult.FAILED;
	}

	private void adicionarAutorizacao(HttpHeaders headers, String accessToken) {
		if (accessToken != null && !accessToken.isBlank()) {
			headers.setBearerAuth(accessToken);
		}
	}
}

