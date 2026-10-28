package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.response.PatientAccessResponse;
import br.com.fiap.arkive.dto.response.PatientShareCreatedResponse;
import br.com.fiap.arkive.dto.response.PatientSharePreviewResponse;
import br.com.fiap.arkive.dto.response.PatientShareResponse;
import br.com.fiap.arkive.entity.PatientAccessRole;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.PatientAccessService;
import br.com.fiap.arkive.service.PatientShareService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PatientSharingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PatientSharingControllerMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PatientShareService shareService;

	@MockitoBean
	private PatientAccessService accessService;

	@MockitoBean
	private ArkiveUserDetailsService arkiveUserDetailsService;

	@Test
	void anonymousCanPreviewWithoutConsumingButCannotRedeem() throws Exception {
		LocalDateTime expiresAt = LocalDateTime.of(2026, 10, 6, 12, 0);
		when(shareService.preview("secret-token")).thenReturn(new PatientSharePreviewResponse(
				true, "PENDING", "Nina", "Canino", PatientAccessRole.RESPONSAVEL, "Dra Vera", expiresAt));

		mockMvc.perform(post("/api/compartilhamentos/preview")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"token\":\"secret-token\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.patientName").value("Nina"))
				.andExpect(jsonPath("$.invitationType").value("RESPONSAVEL"));

		mockMvc.perform(post("/api/compartilhamentos/redeem")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"token\":\"secret-token\"}"))
				.andExpect(status().isUnauthorized());
		verify(shareService).preview("secret-token");
	}

	@Test
	void authenticatedUserCanCreateAndRedeemInvitation() throws Exception {
		UsuarioPrincipal principal = veterinarian();
		LocalDateTime expiresAt = LocalDateTime.of(2026, 10, 6, 12, 0);
		when(shareService.create(eq(50L), any(), same(principal))).thenReturn(
				new PatientShareCreatedResponse(90L, "raw-secret", PatientAccessRole.VETERINARIO, expiresAt));
		when(shareService.redeem("raw-secret", principal)).thenReturn(accessResponse());

		mockMvc.perform(post("/api/animais/50/compartilhamentos")
					.with(user(principal))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"invitationType\":\"VETERINARIO\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.invitationId").value(90))
				.andExpect(jsonPath("$.rawToken").value("raw-secret"));

		mockMvc.perform(post("/api/compartilhamentos/redeem")
					.with(user(principal))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"token\":\"raw-secret\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.animalId").value(50))
				.andExpect(jsonPath("$.accessRole").value("VETERINARIO"));
	}

	@Test
	void authenticatedManagementEndpointsDelegateWithPatientScope() throws Exception {
		UsuarioPrincipal principal = veterinarian();
		PatientShareResponse invitation = invitationResponse();
		PatientAccessResponse access = accessResponse();
		when(shareService.list(50L, principal)).thenReturn(List.of(invitation));
		when(shareService.revoke(90L, principal)).thenReturn(invitation);
		when(accessService.list(50L, principal)).thenReturn(List.of(access));
		when(accessService.revoke(50L, 70L, principal)).thenReturn(access);

		mockMvc.perform(get("/api/animais/50/compartilhamentos").with(user(principal)))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].invitationId").value(90));
		mockMvc.perform(delete("/api/compartilhamentos/90").with(user(principal)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
		mockMvc.perform(get("/api/animais/50/acessos").with(user(principal)))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(70));
		mockMvc.perform(delete("/api/animais/50/acessos/70").with(user(principal)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(70));
	}

	private PatientAccessResponse accessResponse() {
		return new PatientAccessResponse(70L, 50L, "ARK-PET-22222-33333-44444-55555-666666",
				2L, "Dr Caio", PatientAccessRole.VETERINARIO, 1L,
				LocalDateTime.of(2026, 10, 5, 12, 0), null, null, null, true);
	}

	private PatientShareResponse invitationResponse() {
		return new PatientShareResponse(90L, "ARK-PET-22222-33333-44444-55555-666666",
				PatientAccessRole.VETERINARIO, 1L, "Dra Vera",
				LocalDateTime.of(2026, 10, 5, 12, 0), LocalDateTime.of(2026, 10, 6, 12, 0),
				null, null, null, "PENDING");
	}

	private UsuarioPrincipal veterinarian() {
		return new UsuarioPrincipal(1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash",
				TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
	}
}
