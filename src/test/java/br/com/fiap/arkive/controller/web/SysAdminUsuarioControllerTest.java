package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.dto.response.UsuarioContextOption;
import br.com.fiap.arkive.dto.response.UsuarioResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-nodb")
class SysAdminUsuarioControllerTest {

	private final MockMvc mockMvc;
	private final RequestMappingHandlerMapping handlerMapping;

	@MockitoBean
	private UsuarioService usuarioService;

	@Autowired
	SysAdminUsuarioControllerTest(MockMvc mockMvc, RequestMappingHandlerMapping handlerMapping) {
		this.mockMvc = mockMvc;
		this.handlerMapping = handlerMapping;
	}

	@BeforeEach
	void setUp() {
		when(usuarioService.listar(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(usuarioService.listarClinicasAtivas()).thenReturn(List.of(new UsuarioContextOption(1L, "Clínica Central")));
		when(usuarioService.listarVeterinariosAtivos()).thenReturn(List.of(new UsuarioContextOption(2L, "Dra. Vera")));
		when(usuarioService.listarResponsaveisAtivos()).thenReturn(List.of(new UsuarioContextOption(3L, "Rui Responsável")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void sysadminAcessaListagem() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Usuários")))
				.andExpect(content().string(containsString("Novo usuário")));
	}

	@Test
	@WithMockUser(roles = "ADMIN_CLINICA")
	void adminClinicaNaoAcessaListagemSysadmin() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "VETERINARIO")
	void veterinarioNaoAcessaListagemSysadmin() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "RESPONSAVEL")
	void responsavelNaoAcessaListagemSysadmin() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isForbidden());
	}

	@Test
	void anonimoRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void listagemRenderizaCamposSegurosESidebar() throws Exception {
		UsuarioResponse usuario = new UsuarioResponse(
				10L,
				"Ana Sys",
				TipoUsuario.SYSADMIN,
				"ana@arkive.com",
				"S",
				LocalDateTime.of(2026, 8, 24, 10, 0),
				null,
				null,
				null,
				null,
				null,
				null
		);
		when(usuarioService.listar(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(usuario)));

		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Ana Sys")))
				.andExpect(content().string(containsString("ana@arkive.com")))
				.andExpect(content().string(containsString("SysAdmin")))
				.andExpect(content().string(containsString("Ativo")))
				.andExpect(content().string(containsString("Usuários")))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("$2a$"))));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioNovoRenderizaPerfisEOpcoesDeContexto() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios/novo"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("SysAdmin")))
				.andExpect(content().string(containsString("Administrador da Clínica")))
				.andExpect(content().string(containsString("Veterinário")))
				.andExpect(content().string(containsString("Responsável")))
				.andExpect(content().string(containsString("Clínica Central")))
				.andExpect(content().string(containsString("Dra. Vera")))
				.andExpect(content().string(containsString("Rui Responsável")));
	}

	@Test
	void criarUsuarioExigeCsrf() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
		verify(usuarioService, never()).criar(any());
	}

	@Test
	void criarUsuarioValidoChamaServiceERedireciona() throws Exception {
		ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);

		mockMvc.perform(post("/sysadmin/usuarios")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "Novo Admin")
						.param("login", "novo@arkive.com")
						.param("senha", "senha-segura")
						.param("tipo", "SYSADMIN")
						.param("ativo", "S"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/usuarios"));

		verify(usuarioService).criar(captor.capture());
		assertEquals("Novo Admin", captor.getValue().nome());
		assertEquals("novo@arkive.com", captor.getValue().login());
		assertEquals("senha-segura", captor.getValue().senha());
		assertEquals(TipoUsuario.SYSADMIN, captor.getValue().tipo());
	}

	@Test
	void criarUsuarioComBeanValidationInvalidaRenderizaFormulario() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "")
						.param("login", "")
						.param("senha", "curta")
						.param("tipo", "")
						.param("ativo", "S"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo usuário")));

		verify(usuarioService, never()).criar(any());
	}

	@Test
	void criarUsuarioComErroDeNegocioRenderizaFormularioSemSenha() throws Exception {
		doThrow(new BusinessException("Login de usuario ja cadastrado.")).when(usuarioService).criar(any());

		mockMvc.perform(post("/sysadmin/usuarios")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "Novo Admin")
						.param("login", "novo@arkive.com")
						.param("senha", "senha-segura")
						.param("tipo", "SYSADMIN")
						.param("ativo", "S"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Login de usuario ja cadastrado.")))
				.andExpect(content().string(not(containsString("value=\"senha-segura\""))))
				.andExpect(content().string(not(containsString("senhaHash"))));
	}

	@Test
	void desativarUsuarioExigeCsrf() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios/10/desativar").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
		verify(usuarioService, never()).desativar(any(), any());
	}

	@Test
	void desativarUsuarioChamaServiceComUsuarioAtual() throws Exception {
		UsuarioPrincipal principal = new UsuarioPrincipal(99L, "Ana Sys", "ana@arkive.com", "$2a$10$hash", TipoUsuario.SYSADMIN, "S");

		mockMvc.perform(post("/sysadmin/usuarios/10/desativar")
						.with(user(principal))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/usuarios"));

		verify(usuarioService).desativar(10L, 99L);
	}

	@Test
	void ativarUsuarioChamaService() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios/10/ativar")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/usuarios"));

		verify(usuarioService).ativar(10L);
	}

	@Test
	void naoExisteRotaGetParaMudancaDeEstado() {
		boolean hasGetStateRoute = handlerMapping.getHandlerMethods().keySet().stream()
				.map(RequestMappingInfo::toString)
				.anyMatch(mapping -> mapping.contains("GET") && (mapping.contains("/desativar") || mapping.contains("/ativar")));

		assertTrue(!hasGetStateRoute);
	}

}
