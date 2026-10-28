package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.response.VeterinarioCustoResponse;
import br.com.fiap.arkive.dto.response.VeterinarioCustoResponse.ConsultaCusto;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.VeterinarioCustoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VeterinarioCustoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class VeterinarioCustoControllerMvcTest {

	@Autowired MockMvc mvc;
	@MockitoBean VeterinarioCustoService service;
	@MockitoBean ArkiveUserDetailsService usuarios;

	@Test
	void veterinarioConsultaDashboardDaIdentidadeAutenticada() throws Exception {
		UsuarioPrincipal principal = veterinario(17L);
		when(service.buscarCustosAutenticados(principal)).thenReturn(new VeterinarioCustoResponse(
				17L, "Dra. Fernanda", new BigDecimal("100.00"), new BigDecimal("40.00"),
				new BigDecimal("140.00"), 1, 1, List.of(new ConsultaCusto(
				42L, LocalDateTime.parse("2026-10-20T14:30:00"), 123L, "Epona", "ARK-PET-EPONA",
				new BigDecimal("100.00"), new BigDecimal("40.00"), new BigDecimal("140.00")))));

		mvc.perform(get("/api/veterinarios/me/custos").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.veterinarioId").value(17))
				.andExpect(jsonPath("$.totalGeral").value(140.00))
				.andExpect(jsonPath("$.consultas[0].animalNome").value("Epona"))
				.andExpect(jsonPath("$.consultas[0].animalArkivePatientId").value("ARK-PET-EPONA"));

		verify(service).buscarCustosAutenticados(principal);
	}

	@Test
	void dashboardEhExclusivoDeVeterinarioAutenticado() throws Exception {
		mvc.perform(get("/api/veterinarios/me/custos"))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/veterinarios/me/custos").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/veterinarios/me/custos").with(user("admin").roles("ADMIN_CLINICA")))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/veterinarios/me/custos").with(user("sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());

		verify(service, never()).buscarCustosAutenticados(org.mockito.ArgumentMatchers.any());
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra. Fernanda", "fernanda@arkive.com", "hash",
				TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}
}
