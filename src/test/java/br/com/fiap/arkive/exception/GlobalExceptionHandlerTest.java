package br.com.fiap.arkive.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

	@Test
	void accessDeniedRetornaForbidden() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/consultas/1/finalizar");

		var response = handler.handleAccessDenied(new AccessDeniedException("Usuario nao autorizado."), request);

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertEquals(403, response.getBody().status());
		assertEquals("Usuario nao autorizado.", response.getBody().message());
		assertEquals("/api/consultas/1/finalizar", response.getBody().path());
	}

}
