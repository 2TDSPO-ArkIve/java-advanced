package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.dto.response.UsuarioResponse;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

	private UsuarioRepository usuarioRepository;
	private ResponsavelRepository responsavelRepository;
	private VeterinarioRepository veterinarioRepository;
	private ClinicaRepository clinicaRepository;
	private PasswordEncoder passwordEncoder;
	private UsuarioService usuarioService;

	@BeforeEach
	void setUp() {
		usuarioRepository = mock(UsuarioRepository.class);
		responsavelRepository = mock(ResponsavelRepository.class);
		veterinarioRepository = mock(VeterinarioRepository.class);
		clinicaRepository = mock(ClinicaRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		usuarioService = new UsuarioService(
				usuarioRepository,
				responsavelRepository,
				veterinarioRepository,
				clinicaRepository,
				passwordEncoder
		);
		when(usuarioRepository.existsByLogin("usuario@arkive.com")).thenReturn(false);
	}

	@Test
	void sysadminAceitaSemAssociacao() {
		assertDoesNotThrow(() -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, null, null, null)));
	}

	@Test
	void sysadminRejeitaQualquerAssociacao() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, 1L, null, null)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, null, 1L, null)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, null, null, 1L)));
	}

	@Test
	void adminClinicaExigeClinica() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, null, null, null)));
	}

	@Test
	void adminClinicaRejeitaResponsavelOuVeterinario() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, 1L, null, 1L)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, null, 1L, 1L)));
	}

	@Test
	void adminClinicaAceitaClinicaAtiva() {
		when(clinicaRepository.findById(1L)).thenReturn(Optional.of(clinicaAtiva()));

		assertDoesNotThrow(() -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, null, null, 1L)));
	}

	@Test
	void veterinarioExigeVeterinario() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.VETERINARIO, null, null, null)));
	}

	@Test
	void veterinarioRejeitaResponsavelOuClinica() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.VETERINARIO, 1L, 1L, null)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.VETERINARIO, null, 1L, 1L)));
	}

	@Test
	void veterinarioAceitaVeterinarioAtivo() {
		when(veterinarioRepository.findById(1L)).thenReturn(Optional.of(veterinarioAtivo()));

		assertDoesNotThrow(() -> usuarioService.validarCriacao(request(TipoUsuario.VETERINARIO, null, 1L, null)));
	}

	@Test
	void responsavelExigeResponsavel() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, null, null, null)));
	}

	@Test
	void responsavelRejeitaVeterinarioOuClinica() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, 1L, 1L, null)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, 1L, null, 1L)));
	}

	@Test
	void responsavelAceitaResponsavelAtivo() {
		when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavelAtivo()));

		assertDoesNotThrow(() -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, 1L, null, null)));
	}

	@Test
	void rejeitaLoginDuplicado() {
		when(usuarioRepository.existsByLogin("usuario@arkive.com")).thenReturn(true);

		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, null, null, null)));
	}

	@Test
	void rejeitaReferenciaInexistente() {
		when(clinicaRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, null, null, 1L)));
	}

	@Test
	void rejeitaReferenciaInativa() {
		Responsavel responsavel = responsavelAtivo();
		responsavel.setAtivo("N");
		when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavel));

		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, 1L, null, null)));
	}

	@Test
	void rejeitaAtivoInvalido() {
		UsuarioRequest request = new UsuarioRequest(
				"Usuario Teste",
				TipoUsuario.SYSADMIN,
				"usuario@arkive.com",
				"senha-segura",
				null,
				null,
				null,
				""
		);

		assertThrows(BusinessException.class, () -> usuarioService.criar(request));
		verify(passwordEncoder, never()).encode(any());
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void criarCodificaSenhaAntesDePersistir() {
		when(passwordEncoder.encode("senha-segura")).thenReturn("$2a$10$hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		UsuarioResponse response = usuarioService.criar(request(TipoUsuario.SYSADMIN, null, null, null));

		verify(passwordEncoder).encode("senha-segura");
		verify(usuarioRepository).save(captor.capture());
		Usuario salvo = captor.getValue();
		Assertions.assertEquals("$2a$10$hash", salvo.getSenhaHash());
		Assertions.assertNotEquals("senha-segura", salvo.getSenhaHash());
		Assertions.assertEquals("S", salvo.getAtivo());
		Assertions.assertEquals("usuario@arkive.com", response.login());
		Assertions.assertTrue(Arrays.stream(UsuarioResponse.class.getRecordComponents())
				.noneMatch(component -> component.getName().toLowerCase().contains("senha")));
	}

	@Test
	void criarNaoPersisteQuandoLoginDuplicado() {
		when(usuarioRepository.existsByLogin("usuario@arkive.com")).thenReturn(true);

		assertThrows(BusinessException.class, () -> usuarioService.criar(request(TipoUsuario.SYSADMIN, null, null, null)));
		verify(passwordEncoder, never()).encode(any());
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void criarMantemValidacaoDeAssociacao() {
		assertThrows(BusinessException.class, () -> usuarioService.criar(request(TipoUsuario.ADMIN_CLINICA, null, null, null)));
		verify(passwordEncoder, never()).encode(any());
		verify(usuarioRepository, never()).save(any());
	}

	private UsuarioRequest request(TipoUsuario tipo, Long responsavelId, Long veterinarioId, Long clinicaId) {
		return new UsuarioRequest(
				"Usuario Teste",
				tipo,
				"usuario@arkive.com",
				"senha-segura",
				responsavelId,
				veterinarioId,
				clinicaId,
				"S"
		);
	}

	private Responsavel responsavelAtivo() {
		Responsavel responsavel = new Responsavel();
		responsavel.setAtivo("S");
		return responsavel;
	}

	private Veterinario veterinarioAtivo() {
		Veterinario veterinario = new Veterinario();
		veterinario.setAtivo("S");
		return veterinario;
	}

	private Clinica clinicaAtiva() {
		Clinica clinica = new Clinica();
		clinica.setAtivo("S");
		return clinica;
	}

}
