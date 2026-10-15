package br.com.fiap.arkive.config;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestEndpoints.class)
@ActiveProfiles("local-nodb")
@Import({ SecurityConfig.class, SecurityConfigTest.TestEndpointConfig.class })
class SecurityConfigTest {

	private static final String PRODUCTION_FRONTEND_ORIGIN =
			"https://mobile-application-sooty.vercel.app";

	private final MockMvc mockMvc;
	private final CorsConfigurationSource corsConfigurationSource;

	@Autowired
	SecurityConfigTest(
			MockMvc mockMvc,
			CorsConfigurationSource corsConfigurationSource
	) {
		this.mockMvc = mockMvc;
		this.corsConfigurationSource = corsConfigurationSource;
	}

	@Test
	void anonimoAcessaHealth() throws Exception {
		mockMvc.perform(get("/api/health")).andExpect(status().isOk());
	}

	@Test
	void anonimoNaoAcessaApiProtegida() throws Exception {
		mockMvc.perform(get("/api/protegido"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(header().string("WWW-Authenticate", "Basic realm=\"ArkIve API\""))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Autenticacao obrigatoria."))
				.andExpect(jsonPath("$.path").value("/api/protegido"));
	}

	@Test
	void anonimoNaoAcessaEscritasGenericasDeConsulta() throws Exception {
		mockMvc.perform(post("/api/consultas")).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/consultas/1")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/consultas/1")).andExpect(status().isUnauthorized());
	}

	@Test
	void somentePreviewDoConviteEhPublico() throws Exception {
		mockMvc.perform(post("/api/compartilhamentos/preview"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/compartilhamentos/redeem"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/animais/1/compartilhamentos"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void somenteRotasExatasDeCadastroPublicoSaoAnonimas() throws Exception {
		mockMvc.perform(post("/api/auth/register")).andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/register/responsavel")).andExpect(status().isOk());
		mockMvc.perform(get("/api/auth/register/responsavel")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/auth/register/sysadmin")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void corsApiIncluiOrigemDeProducaoSemRemoverDesenvolvimento() {
		MockHttpServletRequest request =
				new MockHttpServletRequest("GET", "/api/consultas");

		CorsConfiguration configuration =
				corsConfigurationSource.getCorsConfiguration(request);

		assertNotNull(configuration);
		assertEquals(
				List.of(PRODUCTION_FRONTEND_ORIGIN),
				configuration.getAllowedOrigins()
		);
		assertEquals(
				List.of("http://localhost:*", "http://127.0.0.1:*"),
				configuration.getAllowedOriginPatterns()
		);
		assertEquals(
				List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
				configuration.getAllowedMethods()
		);
		assertEquals(
				List.of("Authorization", "Content-Type", "Accept", "Origin"),
				configuration.getAllowedHeaders()
		);
		assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
	}

	@Test
	void preflightApiAceitaOrigemDeProducaoComAuthorization() throws Exception {
		mockMvc.perform(options("/api/consultas")
						.header("Origin", PRODUCTION_FRONTEND_ORIGIN)
						.header("Access-Control-Request-Method", "GET")
						.header("Access-Control-Request-Headers", "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						"Access-Control-Allow-Origin",
						PRODUCTION_FRONTEND_ORIGIN
				))
				.andExpect(header().string(
						"Access-Control-Allow-Credentials",
						"true"
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("GET")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("POST")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("PUT")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("PATCH")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("DELETE")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("OPTIONS")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Headers",
						containsString("authorization")
				));
	}

	@Test
	void preflightApiNaoAceitaOrigemDesconhecida() throws Exception {
		mockMvc.perform(options("/api/consultas")
						.header("Origin", "https://unknown.example")
						.header("Access-Control-Request-Method", "GET")
						.header("Access-Control-Request-Headers", "authorization"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void anonimoAcessaSwaggerEOpenApi() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void trocaObrigatoriaBloqueiaApiMasNaoSwaggerEOpenApi() throws Exception {
		UsuarioPrincipal principal = new UsuarioPrincipal(
				1L,
				"Ana Sys",
				"ana@arkive.com",
				"$2a$10$hash",
				TipoUsuario.SYSADMIN,
				"S",
				true
		);

		mockMvc.perform(get("/api/protegido").with(user(principal)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/swagger-ui/index.html").with(user(principal)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui.html").with(user(principal)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs").with(user(principal)))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser
	void metodoNaoSuportadoEmApiRetornaJson405() throws Exception {
		mockMvc.perform(put("/api/adesoes-prescricao/80"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.message").value("Metodo HTTP nao suportado para este recurso."))
				.andExpect(jsonPath("$.path").value("/api/adesoes-prescricao/80"));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void sysadminAcessaSysadminEAdmin() throws Exception {
		mockMvc.perform(get("/sysadmin/painel")).andExpect(status().isOk());
		mockMvc.perform(get("/admin/painel")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "ADMIN_CLINICA")
	void adminClinicaAcessaAdminMasNaoSysadmin() throws Exception {
		mockMvc.perform(get("/admin/painel")).andExpect(status().isOk());
		mockMvc.perform(get("/sysadmin/painel")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "VETERINARIO")
	void veterinarioNaoAcessaAdminOuSysadmin() throws Exception {
		mockMvc.perform(get("/admin/painel")).andExpect(status().isForbidden());
		mockMvc.perform(get("/sysadmin/painel")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "RESPONSAVEL")
	void responsavelNaoAcessaAdminOuSysadmin() throws Exception {
		mockMvc.perform(get("/admin/painel")).andExpect(status().isForbidden());
		mockMvc.perform(get("/sysadmin/painel")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void sysadminPodeCriarClinica() throws Exception {
		mockMvc.perform(post("/api/clinicas")).andExpect(status().isOk());
	}

	@ParameterizedTest
	@ValueSource(strings = { "ADMIN_CLINICA", "VETERINARIO", "RESPONSAVEL" })
	void somenteSysadminPodeCriarClinica(String role) throws Exception {
		mockMvc.perform(post("/api/clinicas").with(user("user").roles(role)))
				.andExpect(status().isForbidden());
	}

	@Test
	void mutacoesAdministrativasRepresentativasSaoProtegidasPorPerfil() throws Exception {
		mockMvc.perform(post("/api/veterinarios").with(user("admin").roles("ADMIN_CLINICA")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/especies").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/protocolos-preventivos").with(user("admin").roles("ADMIN_CLINICA")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/eventos-jornada").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isForbidden());
	}

	@Test
	void recursosDeDominioMantemPermissoesEspecificas() throws Exception {
		mockMvc.perform(get("/api/especies").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/protocolos-preventivos").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/avaliacoes-bem-estar").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/eventos-preventivos").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/alertas").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(patch("/api/alertas/1/ler").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/feedbacks-nps").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/eventos-jornada/animal/1/timeline").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
	}

	@Test
	void responsavelNaoPodeCriarRaca() throws Exception {
		mockMvc.perform(post("/api/racas").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/racas").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
	}

	@Test
	void fluxosClinicosPrincipaisContinuamAutorizados() throws Exception {
		mockMvc.perform(post("/api/consultas").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/adesoes-prescricao").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
	}

	@Test
	void notificacoesEDispositivosExigemAutenticacao() throws Exception {
		mockMvc.perform(get("/api/notificacoes")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/notificacoes/dispositivos")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/notificacoes/dispositivos/1")).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@ValueSource(strings = { "VETERINARIO", "RESPONSAVEL" })
	void usuariosMoveisAcessamSuaCaixaEDispositivos(String role) throws Exception {
		mockMvc.perform(get("/api/notificacoes").with(user("mobile").roles(role)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/notificacoes/dispositivos").with(user("mobile").roles(role)))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/notificacoes/dispositivos/1").with(user("mobile").roles(role)))
				.andExpect(status().isOk());
	}

	@Test
	void solicitacaoAcessoTemRegrasEspecificasPorOperacao() throws Exception {
		mockMvc.perform(post("/api/solicitacoes-acesso").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/solicitacoes-acesso").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isForbidden());

		for (String role : List.of("VETERINARIO", "RESPONSAVEL")) {
			mockMvc.perform(get("/api/solicitacoes-acesso").with(user("mobile").roles(role)))
					.andExpect(status().isOk());
			mockMvc.perform(get("/api/solicitacoes-acesso/91").with(user("mobile").roles(role)))
					.andExpect(status().isOk());
		}

		mockMvc.perform(post("/api/solicitacoes-acesso/91/aprovar")
					.with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/solicitacoes-acesso/91/rejeitar")
					.with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/solicitacoes-acesso/91/aprovar")
					.with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = { "SYSADMIN", "ADMIN_CLINICA" })
	void papeisAdministrativosNaoUsamFluxoDeSolicitacaoDeAcesso(String role) throws Exception {
		mockMvc.perform(post("/api/solicitacoes-acesso").with(user("admin").roles(role)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/solicitacoes-acesso").with(user("admin").roles(role)))
				.andExpect(status().isForbidden());
	}

	@Test
	void solicitacaoAcessoExigeAutenticacaoENaoAceitaMetodosNaoPrevistos() throws Exception {
		mockMvc.perform(post("/api/solicitacoes-acesso")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/solicitacoes-acesso")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/solicitacoes-acesso/91")
					.with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = { "SYSADMIN", "ADMIN_CLINICA" })
	void papeisAdministrativosNaoRecebemAcessoGlobalACaixaPrivada(String role) throws Exception {
		mockMvc.perform(get("/api/notificacoes").with(user("admin").roles(role)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/notificacoes/dispositivos").with(user("admin").roles(role)))
				.andExpect(status().isForbidden());
	}

	@TestConfiguration
	static class TestEndpointConfig {

		@Bean
		TestEndpoints testEndpoints() {
			return new TestEndpoints();
		}

	}

	@RestController
	static class TestEndpoints {

		@GetMapping("/api/health")
		String health() {
			return "ok";
		}

		@GetMapping("/api/protegido")
		String apiProtegida() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/clinicas")
		String criarClinica() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/veterinarios")
		String criarVeterinario() {
			return "ok";
		}

		@GetMapping("/api/especies")
		String listarEspecies() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/especies")
		String criarEspecie() {
			return "ok";
		}

		@GetMapping("/api/protocolos-preventivos")
		String listarProtocolosPreventivos() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/protocolos-preventivos")
		String criarProtocoloPreventivo() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/avaliacoes-bem-estar")
		String criarAvaliacaoBemEstar() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/eventos-preventivos")
		String criarEventoPreventivo() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/racas")
		String criarRaca() {
			return "ok";
		}

		@GetMapping("/api/alertas")
		String listarAlertas() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PatchMapping("/api/alertas/{id}/ler")
		String lerAlerta() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/feedbacks-nps")
		String criarFeedbackNps() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/eventos-jornada")
		String criarEventoJornada() {
			return "ok";
		}

		@GetMapping("/api/eventos-jornada/animal/{animalId}/timeline")
		String timelineAnimal() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/consultas")
		String criarConsulta() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/adesoes-prescricao")
		String registrarAdesao() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/compartilhamentos/preview")
		String previewCompartilhamento() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/compartilhamentos/redeem")
		String resgatarCompartilhamento() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/animais/{animalId}/compartilhamentos")
		String criarCompartilhamento() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/auth/register")
		String registrarVeterinario() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/auth/register/responsavel")
		String registrarResponsavel() {
			return "ok";
		}

		@GetMapping("/api/adesoes-prescricao/{id}")
		String buscarAdesao() {
			return "ok";
		}

		@GetMapping("/api/notificacoes")
		String listarNotificacoes() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/notificacoes/dispositivos")
		String registrarDispositivo() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.DeleteMapping("/api/notificacoes/dispositivos/{id}")
		String removerDispositivo() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/solicitacoes-acesso")
		String criarSolicitacaoAcesso() {
			return "ok";
		}

		@GetMapping("/api/solicitacoes-acesso")
		String listarSolicitacoesAcesso() {
			return "ok";
		}

		@GetMapping("/api/solicitacoes-acesso/{id}")
		String detalharSolicitacaoAcesso() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/solicitacoes-acesso/{id}/aprovar")
		String aprovarSolicitacaoAcesso() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/solicitacoes-acesso/{id}/rejeitar")
		String rejeitarSolicitacaoAcesso() {
			return "ok";
		}

		@GetMapping("/swagger-ui/index.html")
		String swaggerUiIndex() {
			return "ok";
		}

		@GetMapping("/swagger-ui.html")
		String swaggerUiHtml() {
			return "ok";
		}

		@GetMapping("/v3/api-docs")
		String apiDocs() {
			return "ok";
		}

		@GetMapping("/sysadmin/painel")
		String sysadmin() {
			return "ok";
		}

		@GetMapping("/admin/painel")
		String admin() {
			return "ok";
		}

	}

}
