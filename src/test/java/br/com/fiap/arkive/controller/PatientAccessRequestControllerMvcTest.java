package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.request.CreatePatientAccessRequest;
import br.com.fiap.arkive.dto.response.PatientAccessRequestResponse;
import br.com.fiap.arkive.entity.PatientAccessRequestStatus;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.PatientAccessRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(PatientAccessRequestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PatientAccessRequestControllerMvcTest {

	@Autowired MockMvc mvc;
	@MockitoBean PatientAccessRequestService service;
	@MockitoBean ArkiveUserDetailsService usuarios;

	@Test
	void veterinarioCriaSolicitacaoSemEnviarIdentidadeNemReceberNomeDoAnimal() throws Exception {
		UsuarioPrincipal principal = veterinario();
		PatientAccessRequestResponse response = response(PatientAccessRequestStatus.PENDING, null, null, null);
		when(service.criar(eq(new CreatePatientAccessRequest("ARK-PET-ABCDE")), eq(principal)))
				.thenReturn(response);

		mvc.perform(post("/api/solicitacoes-acesso")
					.with(user(principal))
					.contentType("application/json")
					.content("{\"arkivePatientId\":\"ARK-PET-ABCDE\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(91))
				.andExpect(jsonPath("$.arkivePatientId").value("ARK-PET-ABCDE"))
				.andExpect(jsonPath("$.animalNome").doesNotExist())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.resolvedByUserId").doesNotExist());

		verify(service).criar(new CreatePatientAccessRequest("ARK-PET-ABCDE"), principal);
	}

	@Test
	void contratoExigeArkiveIdNaoVazio() throws Exception {
		mvc.perform(post("/api/solicitacoes-acesso")
					.with(user(veterinario()))
					.contentType("application/json")
					.content("{\"arkivePatientId\":\" \"}"))
				.andExpect(status().isBadRequest());

		verify(service, never()).criar(any(), any());
	}

	@Test
	void listaFiltraStatusEUsaContextoDoPrincipal() throws Exception {
		UsuarioPrincipal principal = responsavel();
		when(service.listar(eq(PatientAccessRequestStatus.PENDING), any(), eq(principal)))
				.thenReturn(new PageImpl<>(List.of(response(
						PatientAccessRequestStatus.PENDING, "Epona", null, null))));

		mvc.perform(get("/api/solicitacoes-acesso?status=PENDING").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].animalNome").value("Epona"))
				.andExpect(jsonPath("$.content[0].veterinarioNome").value("Fernanda Vieira"));

		verify(service).listar(eq(PatientAccessRequestStatus.PENDING), any(), eq(principal));
	}

	@Test
	void responsavelPodeDetalharEAprovarOuRejeitar() throws Exception {
		UsuarioPrincipal principal = responsavel();
		when(service.buscar(91L, principal)).thenReturn(response(PatientAccessRequestStatus.PENDING,
				"Epona", null, null));
		when(service.aprovar(91L, principal)).thenReturn(response(PatientAccessRequestStatus.APPROVED,
				"Epona", 40L, 70L));
		when(service.rejeitar(92L, principal)).thenReturn(response(PatientAccessRequestStatus.REJECTED,
				"Epona", 40L, null));

		mvc.perform(get("/api/solicitacoes-acesso/91").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING"));
		mvc.perform(post("/api/solicitacoes-acesso/91/aprovar").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.resultingAccessId").value(70));
		mvc.perform(post("/api/solicitacoes-acesso/92/rejeitar").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void regrasDeUrlRejeitamPapeisIncorretosENaoAutenticado() throws Exception {
		mvc.perform(post("/api/solicitacoes-acesso")
					.with(user(responsavel()))
					.contentType("application/json")
					.content("{\"arkivePatientId\":\"ARK-PET-ABCDE\"}"))
				.andExpect(status().isForbidden());
		mvc.perform(post("/api/solicitacoes-acesso/91/aprovar").with(user(veterinario())))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/solicitacoes-acesso"))
				.andExpect(status().isUnauthorized());
	}

	private PatientAccessRequestResponse response(PatientAccessRequestStatus status, String animalNome,
			Long resolvedBy, Long accessId) {
		return new PatientAccessRequestResponse(91L, "ARK-PET-ABCDE", animalNome, 1L, 17L,
				"Fernanda Vieira", "12345", status, LocalDateTime.parse("2026-10-14T10:00:00"),
				status == PatientAccessRequestStatus.PENDING ? null : LocalDateTime.parse("2026-10-14T11:00:00"),
				resolvedBy, accessId);
	}

	private UsuarioPrincipal veterinario() {
		return new UsuarioPrincipal(1L, "Fernanda Vieira", "vet@example.test", "hash",
				TipoUsuario.VETERINARIO, "S", false, null, 17L, null);
	}

	private UsuarioPrincipal responsavel() {
		return new UsuarioPrincipal(40L, "Responsável", "resp@example.test", "hash",
				TipoUsuario.RESPONSAVEL, "S", false, 8L, null, null);
	}
}
