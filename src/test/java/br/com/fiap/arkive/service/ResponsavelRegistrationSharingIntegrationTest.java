package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.CreatePatientShareRequest;
import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.dto.request.RegistroResponsavelRequest;
import br.com.fiap.arkive.dto.response.ResponsavelResponse;
import br.com.fiap.arkive.entity.*;
import br.com.fiap.arkive.repository.*;
import br.com.fiap.arkive.security.PasswordPolicy;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@Import({ResponsavelRegistrationService.class, PasswordPolicy.class, PatientShareService.class,
		PatientAccessService.class, ClinicalAccessService.class, ShareTokenCodec.class, AnimalService.class,
		ResponsavelRegistrationSharingIntegrationTest.PasswordConfig.class})
class ResponsavelRegistrationSharingIntegrationTest {

	@Autowired TestEntityManager em;
	@Autowired ResponsavelRegistrationService registrationService;
	@Autowired PatientShareService shareService;
	@Autowired AnimalService animalService;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired UsuarioRepository usuarioRepository;
	@Autowired ResponsavelRepository responsavelRepository;
	@Autowired AnimalRepository animalRepository;
	@Autowired AnimalResponsavelRepository relationshipRepository;
	@Autowired PatientAccessRepository accessRepository;
	@Autowired PatientShareTokenRepository tokenRepository;
	@MockitoBean EspecieService especieService;
	@MockitoBean RacaService racaService;
	@MockitoBean ClinicaService clinicaService;
	@MockitoBean EventoJornadaService eventoJornadaService;
	@MockitoBean VeterinarioService veterinarioService;

	@Test
	void registroIsoladoCriaSomenteResponsavelEUsuario() {
		var registration = registrationService.registrar(registrationRequest());
		em.clear();

		Usuario usuario = usuarioRepository.findById(registration.usuarioId()).orElseThrow();
		Responsavel responsavel = responsavelRepository.findById(registration.responsavelId()).orElseThrow();
		assertEquals(TipoUsuario.RESPONSAVEL, usuario.getTipo());
		assertEquals(responsavel.getId(), usuario.getResponsavel().getId());
		assertEquals("RESPONSAVEL", responsavel.getTipo());
		assertEquals(TipoDocumentoResponsavel.CPF, responsavel.getTipoDocumento());
		assertEquals("52998224725", responsavel.getDocumento());
		assertTrue(passwordEncoder.matches("SenhaSegura1", usuario.getSenhaHash()));
		assertEquals("N", usuario.getTrocaSenha());
		assertEquals(0, animalRepository.count());
		assertEquals(0, relationshipRepository.count());
		assertEquals(0, accessRepository.count());
		assertEquals(0, tokenRepository.count());
	}

	@Test
	void registroLegadoSemTipoDocumentoContinuaLegivel() {
		Responsavel legado = new Responsavel();
		legado.setNome("Instituição legada");
		legado.setDocumento("LEGADO-001");
		legado.setTipo("INSTITUICAO");
		legado.setAtivo("S");
		legado.setNotificacao("S");
		em.persistAndFlush(legado);
		em.clear();

		ResponsavelResponse response = ResponsavelResponse.fromEntity(
				responsavelRepository.findById(legado.getId()).orElseThrow());

		assertNull(response.tipoDocumento());
		assertEquals("LEGADO-001", response.documento());
	}

	@Test
	void responsavelCriaAnimalEVinculoPrincipalAtomicamenteSemPatientAccess() {
		var registration = registrationService.registrar(registrationRequest());
		Usuario usuario = usuarioRepository.findById(registration.usuarioId()).orElseThrow();
		UsuarioPrincipal principal = UsuarioPrincipal.fromEntity(usuario);
		Especie especie = new Especie();
		especie.setNome("Equino");
		especie.setAtivo("S");
		em.persistAndFlush(especie);
		when(especieService.buscarEntidade(especie.getId())).thenReturn(especie);

		var criado = animalService.criar(new AnimalRequest(
				"Epona", especie.getId(), null, "F", "N", null, null), principal);
		em.flush();
		em.clear();

		assertNotNull(criado.arkivePatientId());
		assertNull(criado.clinicaId());
		assertEquals("S", criado.ativo());
		assertEquals(1, animalRepository.count());
		assertEquals(1, relationshipRepository.count());
		AnimalResponsavel vinculo = relationshipRepository.findAll().get(0);
		assertEquals(registration.responsavelId(), vinculo.getResponsavel().getId());
		assertEquals("TUTOR_LEGAL", vinculo.getTipoVinculo());
		assertEquals("S", vinculo.getPrincipal());
		assertEquals("S", vinculo.getAtivo());
		assertEquals(0, accessRepository.count());
		assertEquals(1, animalService.listarAutorizado(null, null, null, null, null,
				Pageable.unpaged(), principal).getTotalElements());
		Animal alheio = new Animal();
		alheio.setNome("Epona de outro responsavel");
		alheio.setEspecie(especie);
		alheio.setAtivo("S");
		em.persistAndFlush(alheio);
		assertEquals(1, animalService.listarAutorizado("epo", null, null, null, null,
				Pageable.unpaged(), principal).getTotalElements());
		String trechoArkive = criado.arkivePatientId().substring(0, 16).toLowerCase();
		assertEquals(1, animalService.listarAutorizado(trechoArkive, null, null, null, null,
				Pageable.unpaged(), principal).getTotalElements());
		assertEquals(criado.id(), animalService.buscarPorIdAutorizado(criado.id(), principal).id());
	}

	@Test
	void conviteSobreviveAoCadastroEConcedeAcessoAoMesmoAnimalSomenteAposResgate() {
		Veterinario veterinarian = new Veterinario();
		veterinarian.setNome("Dra Vet");
		veterinarian.setCrmv("SP123");
		veterinarian.setEmail("vet@arkive.com");
		veterinarian.setAtivo("S");
		em.persist(veterinarian);

		Usuario veterinarianUser = new Usuario();
		veterinarianUser.setNome("Dra Vet");
		veterinarianUser.setTipo(TipoUsuario.VETERINARIO);
		veterinarianUser.setVeterinario(veterinarian);
		veterinarianUser.setLogin("vet@arkive.com");
		veterinarianUser.setSenhaHash(passwordEncoder.encode("SenhaDaVet1"));
		veterinarianUser.setAtivo("S");
		veterinarianUser.setTrocaSenha("N");
		em.persist(veterinarianUser);

		Especie species = new Especie();
		species.setNome("Cachorro");
		em.persist(species);
		Animal patient = new Animal();
		patient.setNome("Luna");
		patient.setEspecie(species);
		patient.setAtivo("S");
		patient.setVeterinarioCadastro(veterinarian);
		em.persist(patient);
		em.flush();

		UsuarioPrincipal veterinarianPrincipal = UsuarioPrincipal.fromEntity(veterinarianUser);
		var invitation = shareService.create(patient.getId(),
				new CreatePatientShareRequest(PatientAccessRole.RESPONSAVEL), veterinarianPrincipal);
		var preview = shareService.preview(invitation.rawToken());
		assertTrue(preview.valid());
		assertEquals("PENDING", preview.status());
		assertTrue(usuarioRepository.findByLoginIgnoreCase("maria@arkive.com").isEmpty());
		assertEquals(0, relationshipRepository.count());
		assertEquals(0, accessRepository.count());
		assertNull(tokenRepository.findById(invitation.invitationId()).orElseThrow().getUsedAt());

		var registration = registrationService.registrar(registrationRequest());
		em.flush();
		em.clear();
		Usuario responsibleUser = usuarioRepository.findById(registration.usuarioId()).orElseThrow();
		UsuarioPrincipal responsiblePrincipal = UsuarioPrincipal.fromEntity(responsibleUser);

		assertThrows(org.springframework.security.access.AccessDeniedException.class,
				() -> animalService.buscarPorIdAutorizado(patient.getId(), responsiblePrincipal));
		assertEquals(0, relationshipRepository.count());
		assertEquals(0, accessRepository.count());
		assertNull(tokenRepository.findById(invitation.invitationId()).orElseThrow().getUsedAt());

		var access = shareService.redeem(invitation.rawToken(), responsiblePrincipal);
		em.flush();
		em.clear();

		assertEquals(patient.getId(), access.animalId());
		assertEquals(PatientAccessRole.RESPONSAVEL, access.accessRole());
		assertEquals(patient.getId(), animalService.buscarPorIdAutorizado(patient.getId(), responsiblePrincipal).id());
		assertEquals(1, animalService.listarAutorizado(null, null, null, null, null,
				Pageable.unpaged(), responsiblePrincipal).getTotalElements());
		assertEquals(1, animalRepository.count());
		assertEquals(1, relationshipRepository.count());
		assertEquals("TUTOR_LEGAL", relationshipRepository.findAll().get(0).getTipoVinculo());
		assertEquals("S", relationshipRepository.findAll().get(0).getPrincipal());
		assertEquals(1, accessRepository.findEffective(patient.getId(), responsibleUser.getId(),
				PatientAccessRole.RESPONSAVEL, java.time.LocalDateTime.now()).size());
		PatientShareToken usedInvitation = tokenRepository.findById(invitation.invitationId()).orElseThrow();
		assertNotNull(usedInvitation.getUsedAt());
		assertEquals(responsibleUser.getId(), usedInvitation.getUsedBy().getId());
	}

	private RegistroResponsavelRequest registrationRequest() {
		return new RegistroResponsavelRequest(
				"Maria Responsável", TipoDocumentoResponsavel.CPF, "52998224725",
				"maria@arkive.com", "11999999999",
				"SenhaSegura1", "SenhaSegura1");
	}

	@TestConfiguration
	static class PasswordConfig {
		@Bean
		PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}
	}
}
