package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistroResponsavelRequest;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResponsavelRegistrationServiceTest {

	private ResponsavelRepository responsavelRepository;
	private UsuarioRepository usuarioRepository;
	private PasswordEncoder passwordEncoder;
	private ResponsavelRegistrationService service;

	@BeforeEach
	void setUp() {
		responsavelRepository = mock(ResponsavelRepository.class);
		usuarioRepository = mock(UsuarioRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		service = new ResponsavelRegistrationService(
				responsavelRepository, usuarioRepository, passwordEncoder, new PasswordPolicy());
		when(passwordEncoder.encode("SenhaSegura1")).thenReturn("$2a$10$senhaHash");
		when(responsavelRepository.saveAndFlush(any())).thenAnswer(invocation -> {
			Responsavel responsavel = invocation.getArgument(0);
			responsavel.setId(20L);
			return responsavel;
		});
		when(usuarioRepository.saveAndFlush(any())).thenAnswer(invocation -> {
			Usuario usuario = invocation.getArgument(0);
			usuario.setId(10L);
			return usuario;
		});
	}

	@Test
	void criaResponsavelEUsuarioAtivosComPerfilFixoESenhaEscolhida() {
		var response = service.registrar(request());
		ArgumentCaptor<Responsavel> responsavelCaptor = ArgumentCaptor.forClass(Responsavel.class);
		ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
		verify(responsavelRepository).saveAndFlush(responsavelCaptor.capture());
		verify(usuarioRepository).saveAndFlush(usuarioCaptor.capture());

		Responsavel responsavel = responsavelCaptor.getValue();
		Usuario usuario = usuarioCaptor.getValue();
		assertEquals("Maria Tutor", responsavel.getNome());
		assertEquals("12345678900", responsavel.getDocumento());
		assertEquals("maria@arkive.com", responsavel.getEmail());
		assertEquals("11999999999", responsavel.getTelefone());
		assertEquals("TUTOR", responsavel.getTipo());
		assertEquals("S", responsavel.getNotificacao());
		assertEquals("S", responsavel.getAtivo());

		assertEquals(TipoUsuario.RESPONSAVEL, usuario.getTipo());
		assertSame(responsavel, usuario.getResponsavel());
		assertNull(usuario.getVeterinario());
		assertNull(usuario.getClinica());
		assertEquals("maria@arkive.com", usuario.getLogin());
		assertEquals("$2a$10$senhaHash", usuario.getSenhaHash());
		assertNotEquals("SenhaSegura1", usuario.getSenhaHash());
		assertEquals("S", usuario.getAtivo());
		assertEquals("N", usuario.getTrocaSenha());
		assertNotNull(usuario.getDataUltimaTrocaSenha());
		assertEquals(TipoUsuario.RESPONSAVEL, response.tipo());
		assertFalse(response.trocaSenhaObrigatoria());
		assertEquals(10L, response.usuarioId());
		assertEquals(20L, response.responsavelId());
	}

	@Test
	void normalizaEmailETelefoneEmBrancoSemExporControlesDePerfil() {
		service.registrar(new RegistroResponsavelRequest(
				" Maria Tutor ", " 12345678900 ", " MARIA@ARKIVE.COM ", " ", "SenhaSegura1", "SenhaSegura1"));
		ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
		ArgumentCaptor<Responsavel> responsavelCaptor = ArgumentCaptor.forClass(Responsavel.class);
		verify(usuarioRepository).existsByLoginIgnoreCase("maria@arkive.com");
		verify(usuarioRepository).saveAndFlush(usuarioCaptor.capture());
		verify(responsavelRepository).saveAndFlush(responsavelCaptor.capture());
		assertEquals("maria@arkive.com", usuarioCaptor.getValue().getLogin());
		assertEquals("12345678900", responsavelCaptor.getValue().getDocumento());
		assertNull(responsavelCaptor.getValue().getTelefone());
	}

	@Test
	void rejeitaEmailDuplicadoSemCriarResponsavel() {
		when(usuarioRepository.existsByLoginIgnoreCase("maria@arkive.com")).thenReturn(true);

		BusinessException ex = assertThrows(BusinessException.class, () -> service.registrar(request()));

		assertEquals(HttpStatus.CONFLICT, ex.getStatus());
		verify(responsavelRepository, never()).saveAndFlush(any());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejeitaDocumentoDuplicadoSemCriarUsuario() {
		when(responsavelRepository.existsByDocumento("12345678900")).thenReturn(true);

		BusinessException ex = assertThrows(BusinessException.class, () -> service.registrar(request()));

		assertEquals(HttpStatus.CONFLICT, ex.getStatus());
		verify(responsavelRepository, never()).saveAndFlush(any());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	void aplicaPoliticaAtualDeSenhaAntesDePersistir() {
		RegistroResponsavelRequest weak = new RegistroResponsavelRequest(
				"Maria", "12345678900", "maria@arkive.com", null, "abcdefgh", "abcdefgh");

		assertThrows(BusinessException.class, () -> service.registrar(weak));
		verify(responsavelRepository, never()).saveAndFlush(any());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	void falhaAoCriarUsuarioPropagaParaRollbackDaTransacao() throws Exception {
		doThrow(new IllegalStateException("falha simulada")).when(usuarioRepository).saveAndFlush(any());

		assertThrows(IllegalStateException.class, () -> service.registrar(request()));
		assertTrue(ResponsavelRegistrationService.class
				.getMethod("registrar", RegistroResponsavelRequest.class)
				.isAnnotationPresent(Transactional.class));
	}

	private RegistroResponsavelRequest request() {
		return new RegistroResponsavelRequest(
				"Maria Tutor", "12345678900", "maria@arkive.com", "11999999999",
				"SenhaSegura1", "SenhaSegura1");
	}
}
