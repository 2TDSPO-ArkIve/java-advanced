package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.response.DiagnosticoResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.DiagnosticoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiagnosticoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DiagnosticoControllerMvcTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	DiagnosticoService diagnosticoService;

	@MockitoBean
	ArkiveUserDetailsService arkiveUserDetailsService;

	@Test
	void patchMapeiaUtilTrueERetornaEstadoAtualizado() throws Exception {
		when(diagnosticoService.atualizarValidacaoVeterinaria(eq(50L), eq(true), any(UsuarioPrincipal.class)))
				.thenReturn(response("S"));

		mockMvc.perform(patch("/api/diagnosticos/50/validacao-veterinaria")
						.with(user(veterinario()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"util\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(50))
				.andExpect(jsonPath("$.confirmado").value("N"))
				.andExpect(jsonPath("$.validacaoVet").value("S"));
	}

	@Test
	void patchMapeiaUtilFalse() throws Exception {
		when(diagnosticoService.atualizarValidacaoVeterinaria(eq(50L), eq(false), any(UsuarioPrincipal.class)))
				.thenReturn(response("N"));

		mockMvc.perform(patch("/api/diagnosticos/50/validacao-veterinaria")
						.with(user(veterinario()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"util\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.validacaoVet").value("N"));
	}

	@Test
	void utilAusenteEhRejeitado() throws Exception {
		mockMvc.perform(patch("/api/diagnosticos/50/validacao-veterinaria")
						.with(user(veterinario()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		verify(diagnosticoService, never()).atualizarValidacaoVeterinaria(any(), eq(true), any());
	}

	@Test
	void diagnosticoNaoIaRetorna422() throws Exception {
		when(diagnosticoService.atualizarValidacaoVeterinaria(eq(50L), eq(true), any(UsuarioPrincipal.class)))
				.thenThrow(new BusinessException("A validacao veterinaria e permitida apenas para suporte clinico gerado por IA.",
						HttpStatus.UNPROCESSABLE_ENTITY));

		mockMvc.perform(patch("/api/diagnosticos/50/validacao-veterinaria")
						.with(user(veterinario()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"util\":true}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.status").value(422));
	}

	@Test
	void usuarioNaoAutenticadoRecebe401() throws Exception {
		mockMvc.perform(patch("/api/diagnosticos/50/validacao-veterinaria")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"util\":true}"))
				.andExpect(status().isUnauthorized());
	}

	private DiagnosticoResponse response(String validacaoVet) {
		return new DiagnosticoResponse(50L, "Hipotese", "MODERADA", "N", "Insight", BigDecimal.valueOf(80),
				validacaoVet, 60L, null);
	}

	private UsuarioPrincipal veterinario() {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "hash", TipoUsuario.VETERINARIO,
				"S", false, null, 10L, null);
	}
}
