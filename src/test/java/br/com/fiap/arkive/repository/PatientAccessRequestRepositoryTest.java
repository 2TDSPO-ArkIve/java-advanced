package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.AnimalResponsavelId;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.PatientAccessRequest;
import br.com.fiap.arkive.entity.PatientAccessRequestStatus;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class PatientAccessRequestRepositoryTest {

	@Autowired EntityManager em;
	@Autowired PatientAccessRequestRepository repository;

	@Test
	void listasRespeitamSolicitanteResponsavelVigenteStatusEOrdenacao() {
		Animal primeiroAnimal = animal("Epona", "ARK-PET-REQUEST-A");
		Animal segundoAnimal = animal("Rex", "ARK-PET-REQUEST-B");
		Usuario primeiroVet = veterinario("SP10001", "vet-a@example.test");
		Usuario segundoVet = veterinario("SP10002", "vet-b@example.test");
		Usuario usuarioResponsavel = responsavel();
		Responsavel responsavel = usuarioResponsavel.getResponsavel();
		vincularAtivo(primeiroAnimal, responsavel, null);
		vincularAtivo(segundoAnimal, responsavel, LocalDate.now().minusDays(1));

		PatientAccessRequest antiga = solicitacao(primeiroAnimal, primeiroVet,
				PatientAccessRequestStatus.REJECTED, LocalDateTime.of(2026, 10, 10, 10, 0),
				usuarioResponsavel);
		PatientAccessRequest recente = solicitacao(segundoAnimal, primeiroVet,
				PatientAccessRequestStatus.PENDING, LocalDateTime.of(2026, 10, 14, 10, 0), null);
		PatientAccessRequest outroVeterinario = solicitacao(primeiroAnimal, segundoVet,
				PatientAccessRequestStatus.PENDING, LocalDateTime.of(2026, 10, 13, 10, 0), null);
		em.flush();
		em.clear();

		PageRequest maisRecentes = PageRequest.of(0, 20,
				Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id")));
		var doSolicitante = repository.findForRequester(primeiroVet.getId(), null, maisRecentes);
		assertEquals(2, doSolicitante.getTotalElements());
		assertEquals(recente.getId(), doSolicitante.getContent().get(0).getId());
		assertEquals(antiga.getId(), doSolicitante.getContent().get(1).getId());

		var pendentesDoSolicitante = repository.findForRequester(
				primeiroVet.getId(), PatientAccessRequestStatus.PENDING, maisRecentes);
		assertEquals(1, pendentesDoSolicitante.getTotalElements());
		assertEquals(recente.getId(), pendentesDoSolicitante.getContent().get(0).getId());

		var doResponsavelAtual = repository.findForCurrentResponsible(
				responsavel.getId(), LocalDate.now(), null, maisRecentes);
		assertEquals(2, doResponsavelAtual.getTotalElements());
		assertTrue(doResponsavelAtual.stream().allMatch(r ->
				r.getAnimal().getId().equals(primeiroAnimal.getId())));
		assertEquals(outroVeterinario.getId(), doResponsavelAtual.getContent().get(0).getId());
	}

	@Test
	void detectaPendenteDuplicadaEBuscaDetalhadaCarregaRelacoes() {
		Animal animal = animal("Nina", "ARK-PET-REQUEST-C");
		Usuario vet = veterinario("SP10003", "vet-c@example.test");
		PatientAccessRequest solicitacao = solicitacao(animal, vet,
				PatientAccessRequestStatus.PENDING, LocalDateTime.of(2026, 10, 14, 12, 0), null);
		em.flush();
		em.clear();

		assertTrue(repository.existsByAnimalIdAndRequesterIdAndStatus(
				animal.getId(), vet.getId(), PatientAccessRequestStatus.PENDING));
		PatientAccessRequest detalhada = repository.findDetailedById(solicitacao.getId()).orElseThrow();
		assertEquals("ARK-PET-REQUEST-C", detalhada.getAnimal().getArkivePatientId());
		assertEquals("SP10003", detalhada.getRequester().getVeterinario().getCrmv());
	}

	private Animal animal(String nome, String arkiveId) {
		Especie especie = new Especie();
		especie.setNome("Espécie-" + arkiveId);
		em.persist(especie);
		Animal animal = new Animal();
		animal.setNome(nome);
		animal.setEspecie(especie);
		animal.assignArkivePatientId(arkiveId);
		em.persist(animal);
		return animal;
	}

	private Usuario veterinario(String crmv, String login) {
		Veterinario veterinario = new Veterinario();
		veterinario.setNome("Veterinário " + crmv);
		veterinario.setCrmv(crmv);
		em.persist(veterinario);
		Usuario usuario = new Usuario();
		usuario.setNome(veterinario.getNome());
		usuario.setTipo(TipoUsuario.VETERINARIO);
		usuario.setVeterinario(veterinario);
		usuario.setLogin(login);
		usuario.setSenhaHash("hash");
		em.persist(usuario);
		return usuario;
	}

	private Usuario responsavel() {
		Responsavel responsavel = new Responsavel();
		responsavel.setNome("Responsável");
		responsavel.setDocumento("doc-" + System.nanoTime());
		responsavel.setTipo("RESPONSAVEL");
		em.persist(responsavel);
		Usuario usuario = new Usuario();
		usuario.setNome(responsavel.getNome());
		usuario.setTipo(TipoUsuario.RESPONSAVEL);
		usuario.setResponsavel(responsavel);
		usuario.setLogin("resp@example.test");
		usuario.setSenhaHash("hash");
		em.persist(usuario);
		return usuario;
	}

	private void vincularAtivo(Animal animal, Responsavel responsavel, LocalDate fim) {
		em.flush();
		AnimalResponsavelId id = new AnimalResponsavelId();
		id.setAnimalId(animal.getId());
		id.setResponsavelId(responsavel.getId());
		id.setDataInicio(LocalDate.now().minusDays(10));
		AnimalResponsavel vinculo = new AnimalResponsavel();
		vinculo.setId(id);
		vinculo.setAnimal(animal);
		vinculo.setResponsavel(responsavel);
		vinculo.setTipoVinculo("CUIDADOR");
		vinculo.setAtivo("S");
		vinculo.setDataFim(fim);
		em.persist(vinculo);
	}

	private PatientAccessRequest solicitacao(Animal animal, Usuario requester,
			PatientAccessRequestStatus status, LocalDateTime requestedAt, Usuario resolvedBy) {
		PatientAccessRequest solicitacao = new PatientAccessRequest();
		solicitacao.setAnimal(animal);
		solicitacao.setRequester(requester);
		solicitacao.setStatus(status);
		solicitacao.setRequestedAt(requestedAt);
		if (status == PatientAccessRequestStatus.REJECTED) {
			solicitacao.setResolvedBy(resolvedBy);
			solicitacao.setResolvedAt(requestedAt.plusHours(1));
		}
		em.persist(solicitacao);
		return solicitacao;
	}
}
