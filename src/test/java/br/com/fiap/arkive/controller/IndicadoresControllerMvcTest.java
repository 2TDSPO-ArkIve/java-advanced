package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.response.AnimalConsultasIndicadorResponse;
import br.com.fiap.arkive.dto.response.PacientesAtivosIndicadorResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.exception.IndicadoresDatabaseException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.IndicadoresService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndicadoresController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class IndicadoresControllerMvcTest {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	IndicadoresService indicadoresService;

	@MockitoBean
	ArkiveUserDetailsService usuarios;

	@Test
	void usuarioAutenticadoRecebeIndicadoresDoAnimal() throws Exception {
		UsuarioPrincipal responsavel = principal(TipoUsuario.RESPONSAVEL);
		when(indicadoresService.buscarConsultasAnimal(1L, responsavel))
				.thenReturn(new AnimalConsultasIndicadorResponse(1L, 1L, 1L));

		mvc.perform(get("/api/indicadores/animais/1/consultas").with(user(responsavel)))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.animalId").value(1))
				.andExpect(jsonPath("$.totalConsultas").value(1))
				.andExpect(jsonPath("$.totalConsultasFinalizadas").value(1));
	}

	@Test
	void endpointDoAnimalNaoEhPublico() throws Exception {
		mvc.perform(get("/api/indicadores/animais/1/consultas"))
				.andExpect(status().isUnauthorized());

		verify(indicadoresService, never())
				.buscarConsultasAnimal(org.mockito.ArgumentMatchers.any(),
						org.mockito.ArgumentMatchers.any());
	}

	@Test
	void animalInexistenteRetorna404() throws Exception {
		UsuarioPrincipal responsavel = principal(TipoUsuario.RESPONSAVEL);
		when(indicadoresService.buscarConsultasAnimal(99L, responsavel))
				.thenThrow(new ResourceNotFoundException("Animal nao encontrado."));

		mvc.perform(get("/api/indicadores/animais/99/consultas").with(user(responsavel)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Animal nao encontrado."));
	}

	@Test
	void usuarioSemAcessoAoAnimalRecebe403() throws Exception {
		UsuarioPrincipal responsavel = principal(TipoUsuario.RESPONSAVEL);
		when(indicadoresService.buscarConsultasAnimal(1L, responsavel))
				.thenThrow(new AccessDeniedException("Usuario nao autorizado para este animal."));

		mvc.perform(get("/api/indicadores/animais/1/consultas").with(user(responsavel)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message")
						.value("Usuario nao autorizado para este animal."));
	}

	@Test
	void somenteSysadminRecebeIndicadorGlobal() throws Exception {
		UsuarioPrincipal sysadmin = principal(TipoUsuario.SYSADMIN);
		when(indicadoresService.buscarPacientesAtivos(sysadmin))
				.thenReturn(new PacientesAtivosIndicadorResponse(52L));

		mvc.perform(get("/api/indicadores/pacientes-ativos").with(user(sysadmin)))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.totalPacientesAtivos").value(52));
	}

	@Test
	void indicadorGlobalNaoEhPublico() throws Exception {
		mvc.perform(get("/api/indicadores/pacientes-ativos"))
				.andExpect(status().isUnauthorized());

		verify(indicadoresService, never())
				.buscarPacientesAtivos(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void veterinarioNaoExecutaServiceDoIndicadorGlobal() throws Exception {
		UsuarioPrincipal veterinario = principal(TipoUsuario.VETERINARIO);

		mvc.perform(get("/api/indicadores/pacientes-ativos").with(user(veterinario)))
				.andExpect(status().isForbidden());

		verify(indicadoresService, never()).buscarPacientesAtivos(veterinario);
	}

	@Test
	void falhaOracleRetorna503SemDetalheInterno() throws Exception {
		UsuarioPrincipal sysadmin = principal(TipoUsuario.SYSADMIN);
		when(indicadoresService.buscarPacientesAtivos(sysadmin))
				.thenThrow(new IndicadoresDatabaseException(
						new RuntimeException("ORA-06550 package ausente")));

		mvc.perform(get("/api/indicadores/pacientes-ativos").with(user(sysadmin)))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message")
						.value("Nao foi possivel consultar os indicadores no Oracle."))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ORA-"))));
	}

	private UsuarioPrincipal principal(TipoUsuario tipo) {
		return new UsuarioPrincipal(10L, "Usuario", "usuario@arkive.com", "hash",
				tipo, "S", false, 20L, 30L, 40L);
	}
}
