package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.AnimalResponsavelId;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.PatientAccess;
import br.com.fiap.arkive.entity.PatientAccessRole;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class AnimalRepositoryOracleClobRegressionTest {

	private static final LocalDate DATA_ATUAL = LocalDate.of(2026, 10, 19);

	@Autowired
	TestEntityManager em;

	@Autowired
	AnimalRepository animalRepository;

	@Test
	void responsavelMantemEscopoPatientAccessBuscaEPaginacaoSemDuplicarAnimais() {
		Especie especie = especie("Felino");
		Responsavel responsavel = responsavel("Maria", "doc-responsavel");
		Usuario usuario = usuarioResponsavel(responsavel, "maria@example.test");

		Animal vinculado = animal("Epona", especie);
		vinculado.setObservacaoResponsavelPendente("CLOB não pode participar de DISTINCT no Oracle");
		vinculo(vinculado, responsavel, DATA_ATUAL.minusDays(30), null, "S");
		vinculo(vinculado, responsavel, DATA_ATUAL.minusDays(5), null, "S");

		Animal compartilhado = animal("Luna", especie);
		acesso(compartilhado, usuario, PatientAccessRole.RESPONSAVEL);

		Animal semAcesso = animal("Sem acesso", especie);
		Animal vinculoFuturo = animal("Vínculo futuro", especie);
		vinculo(vinculoFuturo, responsavel, DATA_ATUAL.plusDays(1), null, "S");
		Animal vinculoEncerrado = animal("Vínculo encerrado", especie);
		vinculo(vinculoEncerrado, responsavel, DATA_ATUAL.minusDays(10), DATA_ATUAL.minusDays(1), "S");
		Animal vinculoInativo = animal("Vínculo inativo", especie);
		vinculo(vinculoInativo, responsavel, DATA_ATUAL.minusDays(10), null, "N");
		em.flush();
		em.clear();

		PageRequest primeiraPagina = PageRequest.of(0, 1, Sort.by("id"));
		PageRequest segundaPagina = PageRequest.of(1, 1, Sort.by("id"));
		Page<Animal> primeira = buscarParaResponsavel(responsavel.getId(), null, primeiraPagina);
		Page<Animal> segunda = buscarParaResponsavel(responsavel.getId(), null, segundaPagina);

		assertEquals(2, primeira.getTotalElements());
		assertEquals(2, primeira.getTotalPages());
		assertEquals(2, segunda.getTotalElements());
		Set<Long> idsPaginados = new HashSet<>();
		idsPaginados.add(primeira.getContent().get(0).getId());
		idsPaginados.add(segunda.getContent().get(0).getId());
		assertEquals(Set.of(vinculado.getId(), compartilhado.getId()), idsPaginados);
		assertFalse(idsPaginados.contains(semAcesso.getId()));

		assertEquals(List.of(vinculado.getId()),
				buscarParaResponsavel(responsavel.getId(), "pon", PageRequest.of(0, 10))
						.map(Animal::getId).getContent());
		String trechoArkiveId = compartilhado.getArkivePatientId()
				.substring(0, 12)
				.toLowerCase(Locale.ROOT);
		assertEquals(List.of(compartilhado.getId()),
				buscarParaResponsavel(responsavel.getId(), trechoArkiveId, PageRequest.of(0, 10))
						.map(Animal::getId).getContent());
	}

	@Test
	void veterinarioMantemTodosOsCaminhosDeAcessoBuscaEPaginacaoSemDuplicatas() {
		Especie especie = especie("Canino");
		Clinica clinica = clinica("Clínica A", "12345678000190");
		Veterinario veterinario = veterinario("Ana", "SP100", clinica);
		Veterinario outroVeterinario = veterinario("Bruno", "SP200", null);
		Usuario usuario = usuarioVeterinario(veterinario, "ana.vet@example.test");

		Animal sobreposto = animal("Paciente próprio", especie);
		sobreposto.setVeterinarioCadastro(veterinario);
		sobreposto.setClinica(clinica);
		sobreposto.setObservacaoResponsavelPendente("Campo CLOB preenchido");
		consulta(sobreposto, veterinario);
		consulta(sobreposto, veterinario);
		acesso(sobreposto, usuario, PatientAccessRole.VETERINARIO);

		Animal porConsulta = animal("Paciente por consulta", especie);
		consulta(porConsulta, veterinario);
		consulta(porConsulta, veterinario);

		Animal porClinica = animal("Paciente da clínica", especie);
		porClinica.setClinica(clinica);

		Animal porAcesso = animal("Paciente compartilhado", especie);
		acesso(porAcesso, usuario, PatientAccessRole.VETERINARIO);

		Animal semAcesso = animal("Paciente de outro veterinário", especie);
		semAcesso.setVeterinarioCadastro(outroVeterinario);
		em.flush();
		em.clear();

		PageRequest primeiraPagina = PageRequest.of(0, 2, Sort.by("id"));
		PageRequest segundaPagina = PageRequest.of(1, 2, Sort.by("id"));
		Page<Animal> primeira = buscarParaVeterinario(veterinario.getId(), clinica.getId(), null, primeiraPagina);
		Page<Animal> segunda = buscarParaVeterinario(veterinario.getId(), clinica.getId(), null, segundaPagina);

		assertEquals(4, primeira.getTotalElements());
		assertEquals(2, primeira.getTotalPages());
		Set<Long> idsPaginados = new HashSet<>();
		primeira.map(Animal::getId).forEach(idsPaginados::add);
		segunda.map(Animal::getId).forEach(idsPaginados::add);
		assertEquals(Set.of(sobreposto.getId(), porConsulta.getId(), porClinica.getId(), porAcesso.getId()), idsPaginados);
		assertFalse(idsPaginados.contains(semAcesso.getId()));

		assertEquals(List.of(porConsulta.getId()),
				buscarParaVeterinario(veterinario.getId(), clinica.getId(), "por consulta", PageRequest.of(0, 10))
						.map(Animal::getId).getContent());
		String trechoArkiveId = porAcesso.getArkivePatientId()
				.substring(0, 12)
				.toLowerCase(Locale.ROOT);
		assertEquals(List.of(porAcesso.getId()),
				buscarParaVeterinario(veterinario.getId(), clinica.getId(), trechoArkiveId, PageRequest.of(0, 10))
						.map(Animal::getId).getContent());
		assertTrue(buscarParaVeterinario(null, clinica.getId(), null, PageRequest.of(0, 10)).isEmpty());
	}

	private Page<Animal> buscarParaResponsavel(Long responsavelId, String busca, PageRequest pageable) {
		return animalRepository.buscarParaResponsavel(
				responsavelId, DATA_ATUAL, busca, null, null, null, "S", pageable);
	}

	private Page<Animal> buscarParaVeterinario(
			Long veterinarioId,
			Long clinicaId,
			String busca,
			PageRequest pageable
	) {
		return animalRepository.buscarParaVeterinario(
				veterinarioId, clinicaId, busca, null, null, null, "S", pageable);
	}

	private Especie especie(String nome) {
		Especie especie = new Especie();
		especie.setNome(nome);
		return em.persist(especie);
	}

	private Clinica clinica(String nome, String cnpj) {
		Clinica clinica = new Clinica();
		clinica.setNome(nome);
		clinica.setCnpj(cnpj);
		return em.persist(clinica);
	}

	private Veterinario veterinario(String nome, String crmv, Clinica clinica) {
		Veterinario veterinario = new Veterinario();
		veterinario.setNome(nome);
		veterinario.setCrmv(crmv);
		veterinario.setClinica(clinica);
		return em.persist(veterinario);
	}

	private Responsavel responsavel(String nome, String documento) {
		Responsavel responsavel = new Responsavel();
		responsavel.setNome(nome);
		responsavel.setDocumento(documento);
		responsavel.setTipo("RESPONSAVEL");
		return em.persist(responsavel);
	}

	private Usuario usuarioResponsavel(Responsavel responsavel, String login) {
		Usuario usuario = usuario(login, TipoUsuario.RESPONSAVEL);
		usuario.setResponsavel(responsavel);
		return em.persist(usuario);
	}

	private Usuario usuarioVeterinario(Veterinario veterinario, String login) {
		Usuario usuario = usuario(login, TipoUsuario.VETERINARIO);
		usuario.setVeterinario(veterinario);
		return em.persist(usuario);
	}

	private Usuario usuario(String login, TipoUsuario tipo) {
		Usuario usuario = new Usuario();
		usuario.setNome(login);
		usuario.setLogin(login);
		usuario.setSenhaHash("hash");
		usuario.setTipo(tipo);
		return usuario;
	}

	private Animal animal(String nome, Especie especie) {
		Animal animal = new Animal();
		animal.setNome(nome);
		animal.setEspecie(especie);
		return em.persist(animal);
	}

	private void vinculo(
			Animal animal,
			Responsavel responsavel,
			LocalDate dataInicio,
			LocalDate dataFim,
			String ativo
	) {
		AnimalResponsavelId id = new AnimalResponsavelId();
		id.setAnimalId(animal.getId());
		id.setResponsavelId(responsavel.getId());
		id.setDataInicio(dataInicio);

		AnimalResponsavel vinculo = new AnimalResponsavel();
		vinculo.setId(id);
		vinculo.setAnimal(animal);
		vinculo.setResponsavel(responsavel);
		vinculo.setTipoVinculo("TUTOR_LEGAL");
		vinculo.setDataFim(dataFim);
		vinculo.setAtivo(ativo);
		em.persist(vinculo);
	}

	private void acesso(Animal animal, Usuario usuario, PatientAccessRole role) {
		PatientAccess acesso = new PatientAccess();
		acesso.setAnimal(animal);
		acesso.setUsuario(usuario);
		acesso.setAccessRole(role);
		acesso.setGrantedBy(usuario);
		acesso.setGrantedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
		em.persist(acesso);
	}

	private void consulta(Animal animal, Veterinario veterinario) {
		Consulta consulta = new Consulta();
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setDataHora(LocalDateTime.of(2026, 10, 1, 10, 0));
		consulta.setModalidade("PRESENCIAL");
		consulta.setMotivo("Retorno");
		em.persist(consulta);
	}
}
