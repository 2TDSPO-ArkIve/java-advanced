package br.com.fiap.arkive.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

	@Test
	void validacaoRetornaMapaDeCampos() throws NoSuchMethodException {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/prescricoes");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "prescricaoRequest");
		bindingResult.addError(new FieldError("prescricaoRequest", "medicamento", "nao deve estar em branco"));
		MethodParameter parameter = new MethodParameter(
				GlobalExceptionHandlerTest.class.getDeclaredMethod("prescricaoRequest", String.class),
				0
		);

		var response = handler.handleValidation(new MethodArgumentNotValidException(parameter, bindingResult), request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(400, response.getBody().status());
		assertEquals("nao deve estar em branco", response.getBody().fields().get("medicamento"));
		assertEquals("/api/prescricoes", response.getBody().path());
	}

	@SuppressWarnings("unused")
	private void prescricaoRequest(String medicamento) {
	}

}
