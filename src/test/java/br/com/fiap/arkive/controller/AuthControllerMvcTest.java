package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.response.AuthMeResponse;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.dto.response.RegistroResponsavelResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.TipoDocumentoResponsavel;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AuthControllerMvcTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private ArkiveUserDetailsService arkiveUserDetailsService;

	@Autowired
	AuthControllerMvcTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void trocaSenhaObrigatoriaPermiteConsultarIdentidade() throws Exception {
		when(authService.me(any(UsuarioPrincipal.class))).thenReturn(new AuthMeResponse(
				123L,
				"Dr. Gustavo",
				TipoUsuario.VETERINARIO,
				"gustavo@arkive.com.br",
				true,
				null,
				45L,
				"CRMV1234",
				"gustavo@arkive.com.br",
				null
		));

		mockMvc.perform(get("/api/auth/me").with(user(veterinarioComTrocaObrigatoria())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuarioId").value(123))
				.andExpect(jsonPath("$.tipo").value("VETERINARIO"))
				.andExpect(jsonPath("$.trocaSenhaObrigatoria").value(true))
				.andExpect(jsonPath("$.veterinarioId").value(45))
				.andExpect(jsonPath("$.crmv").value("CRMV1234"))
				.andExpect(jsonPath("$.email").value("gustavo@arkive.com.br"))
				.andExpect(jsonPath("$.clinicaId").value(nullValue()));
	}

	@Test
	void trocaSenhaObrigatoriaPermiteAlterarSenhaPelaApi() throws Exception {
		mockMvc.perform(post("/api/auth/change-password")
						.with(user(veterinarioComTrocaObrigatoria()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"novaSenha\":\"NovaSenha1\"}"))
				.andExpect(status().isNoContent());

		verify(authService).alterarSenha(any(UsuarioPrincipal.class), org.mockito.Mockito.eq("NovaSenha1"));
	}

	@Test
	void trocaSenhaObrigatoriaBloqueiaEndpointClinicoNormal() throws Exception {
		mockMvc.perform(get("/api/consultas").with(user(veterinarioComTrocaObrigatoria())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Troca de senha obrigatoria antes de continuar."));
	}

	@Test
	void cadastroPublicoCriaVeterinarioSemAutenticacao() throws Exception {
		when(authService.registrarVeterinario(any())).thenReturn(new VeterinarioResponse(
				77L, "Dra Nova", "CRMV999", null, "nova@arkive.com", null, null, "S"
		));

		// Deliberately no `.with(user(...))` — proves POST /api/auth/register
		// is reachable by a genuinely anonymous caller, which is the entire
		// point of the registration feature.
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"Dra Nova\",\"crmv\":\"CRMV999\",\"email\":\"nova@arkive.com\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(77))
				.andExpect(jsonPath("$.nome").value("Dra Nova"))
				.andExpect(jsonPath("$.crmv").value("CRMV999"))
				.andExpect(jsonPath("$.email").value("nova@arkive.com"));
	}

	@Test
	void cadastroPublicoValidaCamposObrigatorios() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"\",\"crmv\":\"\",\"email\":\"nao-e-email\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cadastroPublicoPropagaConflitoDeCrmvDuplicado() throws Exception {
		when(authService.registrarVeterinario(any()))
				.thenThrow(new BusinessException("CRMV ja cadastrado.", HttpStatus.CONFLICT));

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"Dra Nova\",\"crmv\":\"CRMV999\",\"email\":\"nova@arkive.com\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("CRMV ja cadastrado."));
	}

	@Test
	void cadastroPublicoDeResponsavelEhAnonimoERetornaRespostaSegura() throws Exception {
		when(authService.registrarResponsavel(any())).thenReturn(new RegistroResponsavelResponse(
				91L, 81L, "Maria Responsável", "maria@arkive.com", TipoUsuario.RESPONSAVEL, "S", false,
				TipoDocumentoResponsavel.CPF));

		mockMvc.perform(post("/api/auth/register/responsavel")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"nome":"Maria Responsável","tipoDocumento":"CPF","documento":"529.982.247-25","email":"maria@arkive.com",
							 "telefone":"11999999999","senha":"SenhaSegura1","confirmacaoSenha":"SenhaSegura1"}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.usuarioId").value(91))
				.andExpect(jsonPath("$.responsavelId").value(81))
				.andExpect(jsonPath("$.tipo").value("RESPONSAVEL"))
				.andExpect(jsonPath("$.tipoDocumento").value("CPF"))
				.andExpect(jsonPath("$.ativo").value("S"))
				.andExpect(jsonPath("$.trocaSenhaObrigatoria").value(false))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void camposArbitrariosDePerfilNaoSaoParteDoContratoPublico() throws Exception {
		when(authService.registrarResponsavel(any())).thenReturn(new RegistroResponsavelResponse(
				91L, 81L, "Maria Responsável", "maria@arkive.com", TipoUsuario.RESPONSAVEL, "S", false,
				TipoDocumentoResponsavel.RG));

		mockMvc.perform(post("/api/auth/register/responsavel")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"nome":"Maria Responsável","tipoDocumento":"RG","documento":"12.345.678-X","email":"maria@arkive.com",
							 "senha":"SenhaSegura1","confirmacaoSenha":"SenhaSegura1",
							 "tipo":"SYSADMIN","role":"ADMIN_CLINICA","clinicaId":99,"veterinarioId":88}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("RESPONSAVEL"));
	}

	@Test
	void cadastroPublicoDeResponsavelValidaPayload() throws Exception {
		mockMvc.perform(post("/api/auth/register/responsavel")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nome\":\"\",\"documento\":\"\",\"email\":\"invalido\",\"senha\":\"curta\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cadastroPublicoDeResponsavelExigeTipoDocumento() throws Exception {
		mockMvc.perform(post("/api/auth/register/responsavel")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"nome":"Maria Responsável","documento":"52998224725","email":"maria@arkive.com",
							 "senha":"SenhaSegura1","confirmacaoSenha":"SenhaSegura1"}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.tipoDocumento").value("Tipo de documento é obrigatório."));
	}

	@Test
	void cadastroPublicoDeResponsavelRejeitaTipoDocumentoInvalido() throws Exception {
		mockMvc.perform(post("/api/auth/register/responsavel")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"nome":"Maria Responsável","tipoDocumento":"CNPJ","documento":"12345678000199",
							 "email":"maria@arkive.com","senha":"SenhaSegura1","confirmacaoSenha":"SenhaSegura1"}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Tipo de documento inválido."));
	}

	@Test
	void cadastroPublicoDeResponsavelRejeitaJsonMalformadoComoErroDoCliente() throws Exception {
		mockMvc.perform(post("/api/auth/register/responsavel")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nome\":\"Maria\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Corpo da requisicao invalido."));
	}

	private UsuarioPrincipal veterinarioComTrocaObrigatoria() {
		return new UsuarioPrincipal(
				123L,
				"Dr. Gustavo",
				"gustavo@arkive.com.br",
				"$2a$10$hash",
				TipoUsuario.VETERINARIO,
				"S",
				true,
				null,
				45L,
				null
		);
	}
}
