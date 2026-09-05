package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.AuthMeResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

	@Test
	void meRetornaIdentidadeDoVeterinarioSemConsultaEComClinicaOpcional() {
		UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
		PasswordLifecycleService passwordLifecycleService = mock(PasswordLifecycleService.class);
		AuthService authService = new AuthService(usuarioRepository, passwordLifecycleService);
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
		AuthService authService = new AuthService(usuarioRepository, passwordLifecycleService);

		authService.alterarSenha(principal(true), "NovaSenha1");

		verify(passwordLifecycleService).alterarSenhaObrigatoria(123L, "NovaSenha1", "NovaSenha1");
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
