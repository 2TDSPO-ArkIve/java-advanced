package br.com.fiap.arkive.service.notification;

import br.com.fiap.arkive.config.ExpoPushProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

class ExpoPushClientTest {

	@Test
	void desabilitadoNaoExecutaHttp() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ExpoPushProperties properties = properties(false);
		ExpoPushClient client = new ExpoPushClient(builder.build(), properties);

		assertEquals(ExpoPushResult.SKIPPED, client.enviar(message()));
		server.verify();
	}

	@Test
	void interpretaTicketComSucesso() {
		Fixture fixture = fixture();
		fixture.server.expect(once(), requestTo(fixture.properties.getUrl()))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						[{"to":"ExponentPushToken[secret]","title":"Titulo","body":"Mensagem",
						"data":{"notificationId":1}}]
						"""))
				.andRespond(withSuccess("{\"data\":[{\"status\":\"ok\",\"id\":\"ticket-1\"}]}",
						MediaType.APPLICATION_JSON));

		assertEquals(ExpoPushResult.SUCCESS, fixture.client.enviar(message()));
		fixture.server.verify();
	}

	@Test
	void identificaDeviceNotRegisteredNoTicketImediato() {
		Fixture fixture = fixture();
		fixture.server.expect(requestTo(fixture.properties.getUrl()))
				.andRespond(withSuccess("""
						{"data":[{"status":"error","message":"not registered",
						"details":{"error":"DeviceNotRegistered"}}]}
						""", MediaType.APPLICATION_JSON));

		assertEquals(ExpoPushResult.DEVICE_NOT_REGISTERED, fixture.client.enviar(message()));
	}

	@Test
	void falhasHttpERespostaInvalidaSaoConvertidasEmFalhaSegura() {
		assertHttpFailure(withTooManyRequests());
		assertHttpFailure(withServerError());
		assertHttpFailure(withBadRequest());

		Fixture fixture = fixture();
		fixture.server.expect(requestTo(fixture.properties.getUrl()))
				.andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
		assertEquals(ExpoPushResult.FAILED, fixture.client.enviar(message()));
	}

	@Test
	void timeoutOuErroDeRedeNaoEscapaDoCliente() {
		ExpoPushProperties properties = properties(true);
		RestClient restClient = RestClient.builder()
				.requestFactory((uri, httpMethod) -> {
					throw new IOException("simulated timeout");
				})
				.build();

		assertEquals(ExpoPushResult.FAILED, new ExpoPushClient(restClient, properties).enviar(message()));
	}

	private void assertHttpFailure(org.springframework.test.web.client.ResponseCreator response) {
		Fixture fixture = fixture();
		fixture.server.expect(requestTo(fixture.properties.getUrl())).andRespond(response);
		assertEquals(ExpoPushResult.FAILED, fixture.client.enviar(message()));
	}

	private Fixture fixture() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ExpoPushProperties properties = properties(true);
		return new Fixture(new ExpoPushClient(builder.build(), properties), server, properties);
	}

	private ExpoPushProperties properties(boolean enabled) {
		ExpoPushProperties properties = new ExpoPushProperties();
		properties.setEnabled(enabled);
		properties.setUrl("https://expo.example.test/push/send");
		return properties;
	}

	private ExpoPushMessage message() {
		return new ExpoPushMessage("ExponentPushToken[secret]", "Titulo", "Mensagem",
				Map.of("notificationId", 1L));
	}

	private record Fixture(ExpoPushClient client, MockRestServiceServer server,
			ExpoPushProperties properties) {
	}
}
