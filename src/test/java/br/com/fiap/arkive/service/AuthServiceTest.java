package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistroVeterinarioRequest;
import br.com.fiap.arkive.dto.request.RegistroResponsavelRequest;
import br.com.fiap.arkive.dto.request.VeterinarioRequest;
import br.com.fiap.arkive.dto.response.AuthMeResponse;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.dto.response.RegistroResponsavelResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.TipoDocumentoResponsavel;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.PasswordPolicy;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

	@Test
	void meRetornaIdentidadeDoVeterinarioSemConsultaEComClinicaOpcional() {
		UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
		PasswordLifecycleService passwordLifecycleService = mock(PasswordLifecycleService.class);
		VeterinarioService veterinarioService = mock(VeterinarioService.class);
		AuthService authService = new AuthService(usuarioRepository, passwordLifecycleService, veterinarioService,
				mock(ResponsavelRegistrationService.class), new PasswordPolicy());
		Usuario usuario = usuarioVeterinario();
		when(usuarioRepository.findById(123L)).thenReturn(Optional.of(usuario));

		AuthMeResponse response = authService.me(principal(true));

		assertEquals(123L, response.usuarioId());
		assertEquals("Dr. Gustavo", response.nome());
		assertEquals(TipoUsuario.VETERINARIO, response.tipo());
		assertEquals("gustavo@arkive.com.br", response.login());
		assertTrue(response.trocaSenhaObrigatoria());
		assertEquals(45L, response.veterinarioId());
		assertEquals("CRMV1234", response.crmv());
		assertEquals("gustavo@arkive.com.br", response.email());
		assertNull(response.clinicaId());
	}

	@Test
	void alterarSenhaUsaUsuarioAutenticadoSemExporHashOuSenha() {
		UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
		PasswordLifecycleService passwordLifecycleService = mock(PasswordLifecycleService.class);
		VeterinarioService veterinarioService = mock(VeterinarioService.class);
		AuthService authService = new AuthService(usuarioRepository, passwordLifecycleService, veterinarioService,
				mock(ResponsavelRegistrationService.class), new PasswordPolicy());

		authService.alterarSenha(principal(true), "NovaSenha1");

		verify(passwordLifecycleService).alterarSenhaObrigatoria(123L, "NovaSenha1", "NovaSenha1");
	}

	@Test
	void registrarVeterinarioDelegaParaVeterinarioServiceComTipoForcadoServidor() {
		UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
		PasswordLifecycleService passwordLifecycleService = mock(PasswordLifecycleService.class);
		VeterinarioService veterinarioService = mock(VeterinarioService.class);
		AuthService authService = new AuthService(usuarioRepository, passwordLifecycleService, veterinarioService,
				mock(ResponsavelRegistrationService.class), new PasswordPolicy());
		VeterinarioResponse resposta = new VeterinarioResponse(77L, "Dra Nova", "CRMV999", null, "nova@arkive.com", null, null, "S");
		when(veterinarioService.criarAutocadastro(any(VeterinarioRequest.class), any())).thenReturn(resposta);
		ArgumentCaptor<VeterinarioRequest> captor = ArgumentCaptor.forClass(VeterinarioRequest.class);

		VeterinarioResponse resultado = authService.registrarVeterinario(
				new RegistroVeterinarioRequest(
						"Dra Nova", "CRMV999", "nova@arkive.com", "SenhaSegura1", "SenhaSegura1")
		);

		verify(veterinarioService).criarAutocadastro(captor.capture(), org.mockito.Mockito.eq("SenhaSegura1"));
		assertEquals("Dra Nova", captor.getValue().nome());
		assertEquals("CRMV999", captor.getValue().crmv());
		assertEquals("nova@arkive.com", captor.getValue().email());
		assertNull(captor.getValue().especialidade());
		assertNull(captor.getValue().clinicaId());
		assertNull(captor.getValue().ativo());
		assertEquals(77L, resultado.id());
	}

	@Test
	void registrarVeterinarioAplicaPoliticaEConfirmacaoAntesDePersistir() {
		VeterinarioService veterinarioService = mock(VeterinarioService.class);
		AuthService authService = new AuthService(
				mock(UsuarioRepository.class),
				mock(PasswordLifecycleService.class),
				veterinarioService,
				mock(ResponsavelRegistrationService.class),
				new PasswordPolicy());

		assertThrows(br.com.fiap.arkive.exception.BusinessException.class,
				() -> authService.registrarVeterinario(new RegistroVeterinarioRequest(
						"Dra Nova", "CRMV999", "nova@arkive.com", "senhasemnumero", "senhasemnumero")));
		assertThrows(br.com.fiap.arkive.exception.BusinessException.class,
				() -> authService.registrarVeterinario(new RegistroVeterinarioRequest(
						"Dra Nova", "CRMV999", "nova@arkive.com", "SenhaSegura1", "SenhaSegura2")));

		verify(veterinarioService, never()).criarAutocadastro(any(), any());
	}

	@Test
	void registrarResponsavelDelegaParaFluxoEspecificoSemPermitirEscolhaDePerfil() {
		UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
		PasswordLifecycleService passwordLifecycleService = mock(PasswordLifecycleService.class);
		VeterinarioService veterinarioService = mock(VeterinarioService.class);
		ResponsavelRegistrationService registrationService = mock(ResponsavelRegistrationService.class);
		AuthService authService = new AuthService(usuarioRepository, passwordLifecycleService, veterinarioService,
				registrationService, new PasswordPolicy());
		RegistroResponsavelRequest request = new RegistroResponsavelRequest(
				"Maria", TipoDocumentoResponsavel.CPF, "52998224725", "maria@arkive.com",
				"11999999999", "SenhaSegura1", "SenhaSegura1");
		RegistroResponsavelResponse expected = new RegistroResponsavelResponse(
				10L, 20L, "Maria", "maria@arkive.com", TipoUsuario.RESPONSAVEL, "S", false);
		when(registrationService.registrar(request)).thenReturn(expected);

		RegistroResponsavelResponse result = authService.registrarResponsavel(request);

		assertEquals(expected, result);
		verify(registrationService).registrar(request);
	}

	private Usuario usuarioVeterinario() {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(45L);
		veterinario.setNome("Dr. Gustavo");
		veterinario.setCrmv("CRMV1234");
		veterinario.setEmail("gustavo@arkive.com.br");
		veterinario.setAtivo("S");

		Usuario usuario = new Usuario();
		usuario.setId(123L);
		usuario.setNome("Dr. Gustavo");
		usuario.setTipo(TipoUsuario.VETERINARIO);
		usuario.setLogin("gustavo@arkive.com.br");
		usuario.setSenhaHash("$2a$10$hash");
		usuario.setAtivo("S");
		usuario.setTrocaSenha("S");
		usuario.setVeterinario(veterinario);
		return usuario;
	}

	private UsuarioPrincipal principal(boolean trocaSenhaObrigatoria) {
		return new UsuarioPrincipal(
				123L,
				"Dr. Gustavo",
				"gustavo@arkive.com.br",
				"$2a$10$hash",
				TipoUsuario.VETERINARIO,
				"S",
				trocaSenhaObrigatoria,
				null,
				45L,
				null
		);
	}
}
