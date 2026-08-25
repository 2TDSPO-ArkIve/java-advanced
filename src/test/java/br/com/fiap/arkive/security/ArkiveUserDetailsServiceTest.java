package br.com.fiap.arkive.security;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArkiveUserDetailsServiceTest {

	private UsuarioRepository usuarioRepository;
	private ArkiveUserDetailsService userDetailsService;

	@BeforeEach
	void setUp() {
		usuarioRepository = mock(UsuarioRepository.class);
		userDetailsService = new ArkiveUserDetailsService(usuarioRepository);
	}

	@Test
	void loginExistenteRetornaPrincipal() {
		when(usuarioRepository.findByLogin("usuario@arkive.com")).thenReturn(Optional.of(usuario("S")));

		UserDetails userDetails = userDetailsService.loadUserByUsername("usuario@arkive.com");

		assertEquals("usuario@arkive.com", userDetails.getUsername());
		assertEquals("$2a$10$hash", userDetails.getPassword());
		assertEquals("ROLE_SYSADMIN", userDetails.getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void loginInexistenteLancaUsernameNotFound() {
		when(usuarioRepository.findByLogin("usuario@arkive.com")).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("usuario@arkive.com"));
	}

	@Test
	void usuarioInativoPermaneceDesabilitado() {
		when(usuarioRepository.findByLogin("usuario@arkive.com")).thenReturn(Optional.of(usuario("N")));

		UserDetails userDetails = userDetailsService.loadUserByUsername("usuario@arkive.com");

		assertFalse(userDetails.isEnabled());
	}

	private Usuario usuario(String ativo) {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNome("Usuario");
		usuario.setLogin("usuario@arkive.com");
		usuario.setSenhaHash("$2a$10$hash");
		usuario.setTipo(TipoUsuario.SYSADMIN);
		usuario.setAtivo(ativo);
		return usuario;
	}

}
