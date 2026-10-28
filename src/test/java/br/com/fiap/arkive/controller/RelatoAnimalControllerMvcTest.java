package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.response.RelatoAnimalResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.RelatoAnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RelatoAnimalController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RelatoAnimalControllerMvcTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private RelatoAnimalService relatoAnimalService;

	@MockitoBean
	private ArkiveUserDetailsService arkiveUserDetailsService;

	@Autowired
	RelatoAnimalControllerMvcTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void responsavelCriaRelatoERecebe201ComContratoCompleto() throws Exception {
		RelatoAnimalResponse response = response();
		when(relatoAnimalService.criar(eq(123L), any(), any())).thenReturn(response);

		mockMvc.perform(post("/api/animais/123/relatos")
					.with(user(responsavel()))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "texto": "Vomitou duas vezes durante a madrugada.",
							  "dataOcorrencia": "2026-10-15",
							  "originadoDeTranscricao": true
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(901))
				.andExpect(jsonPath("$.animalId").value(123))
				.andExpect(jsonPath("$.animalNome").value("Epona"))
				.andExpect(jsonPath("$.animalArkivePatientId").value("ARK-PET-TESTE"))
				.andExpect(jsonPath("$.responsavelId").value(40))
				.andExpect(jsonPath("$.responsavelNome").value("Maria Silva"))
				.andExpect(jsonPath("$.originadoDeTranscricao").value(true));
	}

	@Test
	void validaTextoFlagEDataIsoNoContratoHttp() throws Exception {
		mockMvc.perform(post("/api/animais/123/relatos")
					.with(user(responsavel()))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"texto\":\" \",\"originadoDeTranscricao\":false}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/animais/123/relatos")
					.with(user(responsavel()))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"texto\":\"Relato\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/animais/123/relatos")
					.with(user(responsavel()))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"texto\":\"Relato\",\"dataOcorrencia\":\"15/10/2026\",\"originadoDeTranscricao\":false}"))
				.andExpect(status().isBadRequest());

		verify(relatoAnimalService, never()).criar(any(), any(), any());
	}

	@Test
	void somenteResponsavelPodeCriar() throws Exception {
		String json = "{\"texto\":\"Relato\",\"originadoDeTranscricao\":false}";
		mockMvc.perform(post("/api/animais/123/relatos")
					.with(user(veterinario()))
					.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/animais/123/relatos")
					.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void veterinarioAutorizadoListaComPaginacaoEParametrosFixos() throws Exception {
		PageRequest pageable = PageRequest.of(0, 20);
		when(relatoAnimalService.listar(eq(123L), eq("vomito"), eq(0), eq(20), any()))
				.thenReturn(new PageImpl<>(List.of(response()), pageable, 1));

		mockMvc.perform(get("/api/animais/123/relatos")
					.param("busca", "vomito")
					.with(user(veterinario())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(901))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.number").value(0))
				.andExpect(jsonPath("$.size").value(20));
	}

	@Test
	void rejeitaPaginacaoEBuscaForaDosLimites() throws Exception {
		mockMvc.perform(get("/api/animais/123/relatos").param("page", "-1").with(user(veterinario())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/animais/123/relatos").param("size", "51").with(user(veterinario())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/animais/123/relatos").param("busca", "a".repeat(101)).with(user(veterinario())))
				.andExpect(status().isBadRequest());
	}

	private RelatoAnimalResponse response() {
		return new RelatoAnimalResponse(901L, 123L, "Epona", "ARK-PET-TESTE", 40L, "Maria Silva",
				"Vomitou duas vezes durante a madrugada.", LocalDateTime.of(2026, 10, 16, 9, 15, 22),
				LocalDate.of(2026, 10, 15), true);
	}

	private UsuarioPrincipal responsavel() {
		return new UsuarioPrincipal(2L, "Maria", "maria@test", "hash", TipoUsuario.RESPONSAVEL,
				"S", false, 40L, null, null);
	}

	private UsuarioPrincipal veterinario() {
		return new UsuarioPrincipal(3L, "Vet", "vet@test", "hash", TipoUsuario.VETERINARIO,
				"S", false, null, 10L, null);
	}
}
