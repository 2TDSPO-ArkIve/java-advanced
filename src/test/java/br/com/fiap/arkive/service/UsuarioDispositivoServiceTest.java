package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistrarDispositivoRequest;
import br.com.fiap.arkive.dto.response.UsuarioDispositivoResponse;
import br.com.fiap.arkive.entity.PlataformaDispositivo;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.UsuarioDispositivo;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.UsuarioDispositivoRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioDispositivoServiceTest {

	private UsuarioDispositivoRepository repository;
	private UsuarioRepository usuarioRepository;
	private UsuarioDispositivoService service;
	private Usuario usuario;
	private AtomicLong ids;

	@BeforeEach
	void setUp() {
		repository = mock(UsuarioDispositivoRepository.class);
		usuarioRepository = mock(UsuarioRepository.class);
		service = new UsuarioDispositivoService(repository, usuarioRepository);
		usuario = usuario(10L);
		ids = new AtomicLong(1);
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(repository.save(any())).thenAnswer(invocation -> {
			UsuarioDispositivo dispositivo = invocation.getArgument(0);
			if (dispositivo.getId() == null) dispositivo.setId(ids.getAndIncrement());
			return dispositivo;
		});
	}

	@Test
	void registraParaUsuarioAutenticadoSemAceitarUsuarioIdDoCliente() {
		RegistrarDispositivoRequest request = request("ExponentPushToken[token-a]", "install-a");
		when(repository.findByUsuarioIdAndInstalacaoId(10L, "install-a")).thenReturn(Optional.empty());
		when(repository.findByExpoPushToken("ExponentPushToken[token-a]")).thenReturn(Optional.empty());

		UsuarioDispositivoResponse response = service.registrar(request, principal(10L));

		assertEquals(1L, response.id());
		assertEquals("install-a", response.installationId());
		assertEquals(PlataformaDispositivo.ANDROID, response.platform());
		assertTrue(response.active());
		assertNull(response.revokedAt());
		assertEquals(3, RegistrarDispositivoRequest.class.getRecordComponents().length);
		verify(usuarioRepository).findById(10L);
	}

	@Test
	void mesmaInstalacaoAtualizaTokenPlataformaEReativaSemDuplicar() {
		UsuarioDispositivo existente = dispositivo(7L, usuario, "ExponentPushToken[old]", "install-a");
		existente.setAtivo("N");
		existente.setDataRevogacao(LocalDateTime.now().minusDays(1));
		when(repository.findByUsuarioIdAndInstalacaoId(10L, "install-a")).thenReturn(Optional.of(existente));
		when(repository.findByExpoPushToken("ExponentPushToken[new]")).thenReturn(Optional.empty());

		UsuarioDispositivoResponse response = service.registrar(
				new RegistrarDispositivoRequest("ExponentPushToken[new]", PlataformaDispositivo.IOS, "install-a"),
				principal(10L));

		assertEquals(7L, response.id());
		assertEquals("ExponentPushToken[new]", existente.getExpoPushToken());
		assertEquals(PlataformaDispositivo.IOS, existente.getPlataforma());
		assertEquals("S", existente.getAtivo());
		assertNull(existente.getDataRevogacao());
	}

	@Test
	void instalacoesDiferentesCriamDispositivosIndependentes() {
		when(repository.findByUsuarioIdAndInstalacaoId(any(), any())).thenReturn(Optional.empty());
		when(repository.findByExpoPushToken(any())).thenReturn(Optional.empty());

		var primeiro = service.registrar(request("ExponentPushToken[a]", "install-a"), principal(10L));
		var segundo = service.registrar(request("ExponentPushToken[b]", "install-b"), principal(10L));

		assertEquals(1L, primeiro.id());
		assertEquals(2L, segundo.id());
	}

	@Test
	void tokenAtivoDeOutraContaNaoPodeSerTomado() {
		UsuarioDispositivo outro = dispositivo(8L, usuario(20L), "ExponentPushToken[token-a]", "other");
		when(repository.findByUsuarioIdAndInstalacaoId(10L, "install-a")).thenReturn(Optional.empty());
		when(repository.findByExpoPushToken("ExponentPushToken[token-a]")).thenReturn(Optional.of(outro));

		assertThrows(BusinessException.class,
				() -> service.registrar(request("ExponentPushToken[token-a]", "install-a"), principal(10L)));
		verify(repository, never()).save(any());
	}

	@Test
	void somenteDonoPodeDesativarEOperacaoEhSoftDelete() {
		UsuarioDispositivo proprio = dispositivo(7L, usuario, "ExponentPushToken[token-a]", "install-a");
		when(repository.findByIdAndUsuarioId(7L, 10L)).thenReturn(Optional.of(proprio));

		service.desativar(7L, principal(10L));

		assertEquals("N", proprio.getAtivo());
		assertNotNull(proprio.getDataRevogacao());
		assertNotNull(proprio.getDataUltimoContato());
		assertFalse(UsuarioDispositivoResponse.fromEntity(proprio).active());
		when(repository.findByIdAndUsuarioId(7L, 20L)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> service.desativar(7L, principal(20L)));
	}

	@Test
	void respostaSeguraNaoExpoeToken() {
		assertTrue(java.util.Arrays.stream(UsuarioDispositivoResponse.class.getRecordComponents())
				.noneMatch(component -> component.getName().toLowerCase().contains("token")));
	}

	private RegistrarDispositivoRequest request(String token, String instalacao) {
		return new RegistrarDispositivoRequest(token, PlataformaDispositivo.ANDROID, instalacao);
	}

	private Usuario usuario(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setTipo(TipoUsuario.RESPONSAVEL);
		usuario.setAtivo("S");
		return usuario;
	}

	private UsuarioDispositivo dispositivo(Long id, Usuario dono, String token, String instalacao) {
		UsuarioDispositivo dispositivo = new UsuarioDispositivo();
		dispositivo.setId(id);
		dispositivo.setUsuario(dono);
		dispositivo.setExpoPushToken(token);
		dispositivo.setPlataforma(PlataformaDispositivo.ANDROID);
		dispositivo.setInstalacaoId(instalacao);
		return dispositivo;
	}

	private UsuarioPrincipal principal(Long usuarioId) {
		return new UsuarioPrincipal(usuarioId, "Usuario", "user@example.test", "hash",
				TipoUsuario.RESPONSAVEL, "S");
	}
}
