package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistroResponsavelRequest;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoDocumentoResponsavel;
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
		assertEquals("Maria Responsável", responsavel.getNome());
		assertEquals("52998224725", responsavel.getDocumento());
		assertEquals(TipoDocumentoResponsavel.CPF, responsavel.getTipoDocumento());
		assertEquals("maria@arkive.com", responsavel.getEmail());
		assertEquals("11999999999", responsavel.getTelefone());
		assertEquals("RESPONSAVEL", responsavel.getTipo());
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
		assertEquals(TipoDocumentoResponsavel.CPF, response.tipoDocumento());
	}

	@Test
	void normalizaEmailETelefoneEmBrancoSemExporControlesDePerfil() {
		service.registrar(new RegistroResponsavelRequest(
				" Maria Responsável ", TipoDocumentoResponsavel.CPF, " 529.982.247-25 ",
				" MARIA@ARKIVE.COM ", " ", "SenhaSegura1", "SenhaSegura1"));
		ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
		ArgumentCaptor<Responsavel> responsavelCaptor = ArgumentCaptor.forClass(Responsavel.class);
		verify(usuarioRepository).existsByLoginIgnoreCase("maria@arkive.com");
		verify(usuarioRepository).saveAndFlush(usuarioCaptor.capture());
		verify(responsavelRepository).saveAndFlush(responsavelCaptor.capture());
		assertEquals("maria@arkive.com", usuarioCaptor.getValue().getLogin());
		assertEquals("52998224725", responsavelCaptor.getValue().getDocumento());
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
		when(responsavelRepository.existsByDocumento("52998224725")).thenReturn(true);
		RegistroResponsavelRequest formatado = new RegistroResponsavelRequest(
				"Maria Responsável", TipoDocumentoResponsavel.CPF, "529.982.247-25",
				"maria@arkive.com", "11999999999", "SenhaSegura1", "SenhaSegura1");

		BusinessException ex = assertThrows(BusinessException.class, () -> service.registrar(formatado));

		assertEquals(HttpStatus.CONFLICT, ex.getStatus());
		assertEquals("Documento já cadastrado.", ex.getMessage());
		verify(responsavelRepository, never()).saveAndFlush(any());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	void aplicaPoliticaAtualDeSenhaAntesDePersistir() {
		RegistroResponsavelRequest weak = new RegistroResponsavelRequest(
				"Maria", TipoDocumentoResponsavel.CPF, "52998224725", "maria@arkive.com", null,
				"abcdefgh", "abcdefgh");

		assertThrows(BusinessException.class, () -> service.registrar(weak));
		verify(responsavelRepository, never()).saveAndFlush(any());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	void aceitaCpfValidoFormatadoENormalizaAntesDaUnicidade() {
		service.registrar(new RegistroResponsavelRequest(
				"Maria", TipoDocumentoResponsavel.CPF, "529.982.247-25", "maria@arkive.com", null,
				"SenhaSegura1", "SenhaSegura1"));

		verify(responsavelRepository).existsByDocumento("52998224725");
		ArgumentCaptor<Responsavel> captor = ArgumentCaptor.forClass(Responsavel.class);
		verify(responsavelRepository).saveAndFlush(captor.capture());
		assertEquals("52998224725", captor.getValue().getDocumento());
	}

	@Test
	void rejeitaCpfComDigitosVerificadoresInvalidos() {
		assertCpfInvalido("52998224724");
	}

	@Test
	void rejeitaCpfComTodosOsDigitosIguais() {
		assertCpfInvalido("11111111111");
	}

	@Test
	void rejeitaCpfCurtoOuLongo() {
		assertCpfInvalido("5299822472");
		assertCpfInvalido("529982247250");
	}

	@Test
	void aceitaRgNumericoEAlfanumericoComNormalizacaoGenerica() {
		service.registrar(rgRequest("12.345.678-X"));

		ArgumentCaptor<Responsavel> captor = ArgumentCaptor.forClass(Responsavel.class);
		verify(responsavelRepository).saveAndFlush(captor.capture());
		assertEquals("12345678X", captor.getValue().getDocumento());
		assertEquals(TipoDocumentoResponsavel.RG, captor.getValue().getTipoDocumento());
	}

	@Test
	void rejeitaRgComEstruturaInvalida() {
		for (String rg : new String[] { "....", "1234", "123456789012345", "12@345" }) {
			BusinessException ex = assertThrows(BusinessException.class, () -> service.registrar(rgRequest(rg)));
			assertEquals("RG inválido.", ex.getMessage());
		}
		verify(responsavelRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejeitaTipoDeDocumentoAusenteEmDefesaDeDominio() {
		RegistroResponsavelRequest request = new RegistroResponsavelRequest(
				"Maria", null, "52998224725", "maria@arkive.com", null,
				"SenhaSegura1", "SenhaSegura1");

		BusinessException ex = assertThrows(BusinessException.class, () -> service.registrar(request));

		assertEquals("Tipo de documento é obrigatório.", ex.getMessage());
		verify(responsavelRepository, never()).saveAndFlush(any());
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
				"Maria Responsável", TipoDocumentoResponsavel.CPF, "52998224725",
				"maria@arkive.com", "11999999999",
				"SenhaSegura1", "SenhaSegura1");
	}

	private RegistroResponsavelRequest rgRequest(String rg) {
		return new RegistroResponsavelRequest(
				"Maria Responsável", TipoDocumentoResponsavel.RG, rg,
				"maria@arkive.com", null, "SenhaSegura1", "SenhaSegura1");
	}

	private void assertCpfInvalido(String cpf) {
		RegistroResponsavelRequest request = new RegistroResponsavelRequest(
				"Maria", TipoDocumentoResponsavel.CPF, cpf, "maria@arkive.com", null,
				"SenhaSegura1", "SenhaSegura1");
		BusinessException ex = assertThrows(BusinessException.class, () -> service.registrar(request));
		assertEquals("CPF inválido.", ex.getMessage());
	}
}
