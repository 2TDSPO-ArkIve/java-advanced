package br.com.fiap.arkive.config;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestEndpoints.class)
@ActiveProfiles("local-nodb")
@Import({ SecurityConfig.class, SecurityConfigTest.TestEndpointConfig.class })
class SecurityConfigTest {

	private final MockMvc mockMvc;

	@Autowired
	SecurityConfigTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
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

		@org.springframework.web.bind.annotation.PostMapping("/api/adesoes-prescricao")
		String registrarAdesao() {
			return "ok";
		}

		@GetMapping("/api/adesoes-prescricao/{id}")
		String buscarAdesao(@org.springframework.web.bind.annotation.PathVariable Long id) {
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
