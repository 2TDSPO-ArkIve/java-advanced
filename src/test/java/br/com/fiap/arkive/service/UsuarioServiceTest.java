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
	void criarAdminClinicaValido() {
		Clinica clinica = clinicaAtiva();
		when(clinicaRepository.findById(1L)).thenReturn(Optional.of(clinica));
		when(passwordEncoder.encode("senha-segura")).thenReturn("$2a$10$hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		usuarioService.criar(request(TipoUsuario.ADMIN_CLINICA, null, null, 1L));

		verify(usuarioRepository).save(captor.capture());
		Assertions.assertSame(clinica, captor.getValue().getClinica());
		Assertions.assertEquals("$2a$10$hash", captor.getValue().getSenhaHash());
	}

	@Test
	void criarVeterinarioValido() {
		Veterinario veterinario = veterinarioAtivo();
		when(veterinarioRepository.findById(1L)).thenReturn(Optional.of(veterinario));
		when(passwordEncoder.encode("senha-segura")).thenReturn("$2a$10$hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		usuarioService.criar(request(TipoUsuario.VETERINARIO, null, 1L, null));

		verify(usuarioRepository).save(captor.capture());
		Assertions.assertSame(veterinario, captor.getValue().getVeterinario());
		Assertions.assertEquals("$2a$10$hash", captor.getValue().getSenhaHash());
	}

	@Test
	void criarResponsavelValido() {
		Responsavel responsavel = responsavelAtivo();
		when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavel));
		when(passwordEncoder.encode("senha-segura")).thenReturn("$2a$10$hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		usuarioService.criar(request(TipoUsuario.RESPONSAVEL, 1L, null, null));

		verify(usuarioRepository).save(captor.capture());
		Assertions.assertSame(responsavel, captor.getValue().getResponsavel());
		Assertions.assertEquals("$2a$10$hash", captor.getValue().getSenhaHash());
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

	@Test
	void desativarContaAtivaAlteraParaInativo() {
		Usuario usuario = usuario(10L, TipoUsuario.RESPONSAVEL, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		usuarioService.desativar(10L, 99L);

		Assertions.assertEquals("N", usuario.getAtivo());
		verify(usuarioRepository).save(usuario);
		verify(usuarioRepository, never()).countByTipoAndAtivo(any(), any());
	}

	@Test
	void desativarUsuarioInexistenteRejeita() {
		when(usuarioRepository.findById(10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> usuarioService.desativar(10L, 99L));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void desativarContaAtualRejeita() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		assertThrows(BusinessException.class, () -> usuarioService.desativar(10L, 10L));
		verify(usuarioRepository, never()).save(any());
		verify(usuarioRepository, never()).countByTipoAndAtivo(any(), any());
	}

	@Test
	void desativarUltimoSysadminAtivoRejeita() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(1L);

		assertThrows(BusinessException.class, () -> usuarioService.desativar(10L, 99L));
		verify(usuarioRepository, never()).save(any());
		verify(usuarioRepository).countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S");
	}

	@Test
	void desativarSysadminQuandoExisteOutroAtivo() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(2L);

		usuarioService.desativar(10L, 99L);

		Assertions.assertEquals("N", usuario.getAtivo());
		verify(usuarioRepository).save(usuario);
		verify(usuarioRepository).countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S");
	}

	@Test
	void desativarUsuarioJaInativoRejeita() {
		Usuario usuario = usuario(10L, TipoUsuario.RESPONSAVEL, "N");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		assertThrows(BusinessException.class, () -> usuarioService.desativar(10L, 99L));
		verify(usuarioRepository, never()).save(any());
		verify(usuarioRepository, never()).countByTipoAndAtivo(any(), any());
	}

	@Test
	void ativarContaInativaAlteraParaAtivo() {
		Usuario usuario = usuario(10L, TipoUsuario.RESPONSAVEL, "N");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		usuarioService.ativar(10L);

		Assertions.assertEquals("S", usuario.getAtivo());
		verify(usuarioRepository).save(usuario);
	}

	@Test
	void ativarUsuarioInexistenteRejeita() {
		when(usuarioRepository.findById(10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> usuarioService.ativar(10L));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void ativarUsuarioJaAtivoRejeita() {
		Usuario usuario = usuario(10L, TipoUsuario.RESPONSAVEL, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		assertThrows(BusinessException.class, () -> usuarioService.ativar(10L));
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

	private Usuario usuario(Long id, TipoUsuario tipo, String ativo) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setTipo(tipo);
		usuario.setAtivo(ativo);
		return usuario;
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
