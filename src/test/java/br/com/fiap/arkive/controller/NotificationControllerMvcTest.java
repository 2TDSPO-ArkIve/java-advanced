package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.request.RegistrarDispositivoRequest;
import br.com.fiap.arkive.dto.response.ContagemNotificacoesResponse;
import br.com.fiap.arkive.dto.response.NotificacaoResponse;
import br.com.fiap.arkive.dto.response.NotificacoesLidasResponse;
import br.com.fiap.arkive.dto.response.UsuarioDispositivoResponse;
import br.com.fiap.arkive.entity.PlataformaDispositivo;
import br.com.fiap.arkive.entity.TipoNotificacao;
import br.com.fiap.arkive.entity.TipoRecursoNotificacao;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.NotificacaoService;
import br.com.fiap.arkive.service.UsuarioDispositivoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {NotificacaoController.class, UsuarioDispositivoController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class NotificationControllerMvcTest {

	@Autowired MockMvc mockMvc;
	@MockitoBean NotificacaoService notificacaoService;
	@MockitoBean UsuarioDispositivoService dispositivoService;
	@MockitoBean ArkiveUserDetailsService userDetailsService;

	@Test
	void caixaDeEntradaContagemELeituraDelegamComPrincipalAutenticado() throws Exception {
		UsuarioPrincipal principal = principal();
		NotificacaoResponse response = notificacao();
		when(notificacaoService.listar(eq(true), any(Pageable.class), same(principal)))
				.thenReturn(new PageImpl<>(List.of(response)));
		when(notificacaoService.contarNaoLidas(principal)).thenReturn(new ContagemNotificacoesResponse(1));
		when(notificacaoService.marcarComoLida(90L, principal)).thenReturn(response);
		when(notificacaoService.marcarTodasComoLidas(principal)).thenReturn(new NotificacoesLidasResponse(1));

		mockMvc.perform(get("/api/notificacoes?naoLidas=true&page=0&size=20").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(90))
				.andExpect(jsonPath("$.content[0].animalId").value(12));
		mockMvc.perform(get("/api/notificacoes/nao-lidas/contagem").with(user(principal)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
		mockMvc.perform(patch("/api/notificacoes/90/lida").with(user(principal)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(90));
		mockMvc.perform(patch("/api/notificacoes/lidas").with(user(principal)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.updated").value(1));
	}

	@Test
	void registroDeDispositivoIgnoraUsuarioIdExtraEUsaPrincipal() throws Exception {
		UsuarioPrincipal principal = principal();
		when(dispositivoService.registrar(any(RegistrarDispositivoRequest.class), same(principal)))
				.thenReturn(new UsuarioDispositivoResponse(20L, PlataformaDispositivo.ANDROID,
						"install-1", LocalDateTime.now(), LocalDateTime.now(), true, null));

		mockMvc.perform(post("/api/notificacoes/dispositivos")
					.with(user(principal))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"expoPushToken":"ExponentPushToken[abc_123]","platform":"ANDROID",
							 "installationId":"install-1","usuarioId":999}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(20))
				.andExpect(jsonPath("$.expoPushToken").doesNotExist());

		verify(dispositivoService).registrar(any(RegistrarDispositivoRequest.class), same(principal));
	}

	@Test
	void tokenOuPlataformaInvalidosRetornamQuatrocentos() throws Exception {
		UsuarioPrincipal principal = principal();
		mockMvc.perform(post("/api/notificacoes/dispositivos")
					.with(user(principal)).contentType(MediaType.APPLICATION_JSON)
					.content("{\"expoPushToken\":\"invalido\",\"platform\":\"ANDROID\",\"installationId\":\"i\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/notificacoes/dispositivos")
					.with(user(principal)).contentType(MediaType.APPLICATION_JSON)
					.content("{\"expoPushToken\":\"ExponentPushToken[abc]\",\"platform\":\"WINDOWS\",\"installationId\":\"i\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void desativacaoDelegaIdEProprietarioAutenticado() throws Exception {
		UsuarioPrincipal principal = principal();
		mockMvc.perform(delete("/api/notificacoes/dispositivos/20").with(user(principal)))
				.andExpect(status().isNoContent());
		verify(dispositivoService).desativar(20L, principal);
	}

	private UsuarioPrincipal principal() {
		return new UsuarioPrincipal(10L, "Responsável", "responsavel@example.test", "hash",
				TipoUsuario.RESPONSAVEL, "S", false, 5L, null, null);
	}

	private NotificacaoResponse notificacao() {
		return new NotificacaoResponse(90L, TipoNotificacao.CONSULTA_FINALIZADA,
				"Atualização no ArkIve", "Há uma nova atualização no acompanhamento de Rex.",
				LocalDateTime.of(2026, 10, 9, 10, 0), null, 12L,
				"ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD", TipoRecursoNotificacao.CONSULTA, 77L);
	}
}
