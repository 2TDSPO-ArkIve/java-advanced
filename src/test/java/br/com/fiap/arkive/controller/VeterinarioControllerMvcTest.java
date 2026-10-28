package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.VeterinarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VeterinarioController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class VeterinarioControllerMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VeterinarioService veterinarioService;

	@MockitoBean
	private ArkiveUserDetailsService arkiveUserDetailsService;

	@Test
	void veterinarioAutenticadoConsultaSomenteOProprioPerfil() throws Exception {
		VeterinarioResponse response = new VeterinarioResponse(
				45L, "Dra Vera", "CRMV123", "Felinos", "vera@arkive.com", 9L, "Clínica ArkIve", "S");
		when(veterinarioService.buscarPerfilAutenticado(any())).thenReturn(response);
		UsuarioPrincipal principal = veterinario(45L, true);

		mockMvc.perform(get("/api/veterinarios/me").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(45))
				.andExpect(jsonPath("$.nome").value("Dra Vera"))
				.andExpect(jsonPath("$.crmv").value("CRMV123"))
				.andExpect(jsonPath("$.email").value("vera@arkive.com"))
				.andExpect(jsonPath("$.especialidade").value("Felinos"))
				.andExpect(jsonPath("$.clinicaId").value(9))
				.andExpect(jsonPath("$.clinicaNome").value("Clínica ArkIve"));

		verify(veterinarioService).buscarPerfilAutenticado(principal);
	}

	@Test
	void veterinarioContinuaProibidoNoCrudAdministrativoPorId() throws Exception {
		mockMvc.perform(get("/api/veterinarios/99").with(user(veterinario(45L))))
				.andExpect(status().isForbidden());

		verify(veterinarioService, never()).buscarPorId(any());
	}

	@Test
	void sysadminContinuaAutorizadoNoCrudPorIdMasNaoNoPerfilVeterinario() throws Exception {
		when(veterinarioService.buscarPorId(99L)).thenReturn(new VeterinarioResponse(
				99L, "Dra Outra", "CRMV999", null, "outra@arkive.com", null, null, "S"));

		mockMvc.perform(get("/api/veterinarios/99").with(user("sys").roles("SYSADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(99));
		mockMvc.perform(get("/api/veterinarios/me").with(user("sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
	}

	@Test
	void perfilVeterinarioRejeitaOutrosPapeisENaoAutenticado() throws Exception {
		mockMvc.perform(get("/api/veterinarios/me")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/veterinarios/me").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/veterinarios/me").with(user("admin").roles("ADMIN_CLINICA")))
				.andExpect(status().isForbidden());
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return veterinario(veterinarioId, false);
	}

	private UsuarioPrincipal veterinario(Long veterinarioId, boolean trocaSenhaObrigatoria) {
		return new UsuarioPrincipal(
				1L, "Dra Vera", "vera@arkive.com", "hash", TipoUsuario.VETERINARIO, "S", trocaSenhaObrigatoria,
				null, veterinarioId, 9L);
	}
}
