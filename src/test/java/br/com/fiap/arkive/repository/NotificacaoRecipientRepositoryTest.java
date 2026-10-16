package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.AnimalResponsavelId;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.service.NotificacaoRecipientService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(NotificacaoRecipientService.class)
class NotificacaoRecipientRepositoryTest {

	@Autowired EntityManager em;
	@Autowired NotificacaoRecipientService service;

	@Test
	void responsaveisExcluiVinculosHistoricosInativosEContasInativas() {
		Animal animal = animal();
		Usuario atual = responsavelComVinculo(animal, "atual", "S", "S", null);
		responsavelComVinculo(animal, "historico", "S", "S", LocalDate.now().minusDays(1));
		responsavelComVinculo(animal, "vinculo-inativo", "S", "N", null);
		responsavelComVinculo(animal, "conta-inativa", "N", "S", null);
		em.flush();
		em.clear();

		List<Usuario> destinatarios = service.responsaveisAtivosVigentes(animal);

		assertEquals(List.of(atual.getId()), destinatarios.stream().map(Usuario::getId).toList());
	}

	@Test
	void resolveSomenteContaVeterinariaAtivaLigadaAoVeterinario() {
		Veterinario ativo = veterinario("SP10001");
		Usuario usuarioAtivo = usuarioVeterinario(ativo, "vet-ativo@example.test", "S");
		Veterinario inativo = veterinario("SP10002");
		usuarioVeterinario(inativo, "vet-inativo@example.test", "N");
		em.flush();
		em.clear();

		assertEquals(usuarioAtivo.getId(), service.veterinarioAtivo(ativo).orElseThrow().getId());
		assertTrue(service.veterinarioAtivo(inativo).isEmpty());
	}

	private Animal animal() {
		Especie especie = new Especie();
		especie.setNome("Canino-recipient");
		em.persist(especie);
		Animal animal = new Animal();
		animal.setNome("Epona");
		animal.setEspecie(especie);
		animal.assignArkivePatientId("ARK-PET-RECIPIENT-TEST-00001");
		em.persist(animal);
		em.flush();
		return animal;
	}

	private Usuario responsavelComVinculo(Animal animal, String sufixo, String usuarioAtivo,
			String vinculoAtivo, LocalDate fim) {
		Responsavel responsavel = new Responsavel();
		responsavel.setNome("Responsável " + sufixo);
		responsavel.setDocumento("doc-" + sufixo);
		responsavel.setTipo("RESPONSAVEL");
		em.persist(responsavel);
		em.flush();
		Usuario usuario = new Usuario();
		usuario.setNome(responsavel.getNome());
		usuario.setTipo(TipoUsuario.RESPONSAVEL);
		usuario.setResponsavel(responsavel);
		usuario.setLogin(sufixo + "@example.test");
		usuario.setSenhaHash("hash");
		usuario.setAtivo(usuarioAtivo);
		em.persist(usuario);
		AnimalResponsavelId id = new AnimalResponsavelId();
		id.setAnimalId(animal.getId());
		id.setResponsavelId(responsavel.getId());
		id.setDataInicio(LocalDate.now().minusDays(10));
		AnimalResponsavel vinculo = new AnimalResponsavel();
		vinculo.setId(id);
		vinculo.setAnimal(animal);
		vinculo.setResponsavel(responsavel);
		vinculo.setTipoVinculo("CUIDADOR");
		vinculo.setAtivo(vinculoAtivo);
		vinculo.setDataFim(fim);
		em.persist(vinculo);
		return usuario;
	}

	private Veterinario veterinario(String crmv) {
		Veterinario veterinario = new Veterinario();
		veterinario.setNome("Veterinário " + crmv);
		veterinario.setCrmv(crmv);
		em.persist(veterinario);
		return veterinario;
	}

	private Usuario usuarioVeterinario(Veterinario veterinario, String login, String ativo) {
		Usuario usuario = new Usuario();
		usuario.setNome(veterinario.getNome());
		usuario.setTipo(TipoUsuario.VETERINARIO);
		usuario.setVeterinario(veterinario);
		usuario.setLogin(login);
		usuario.setSenhaHash("hash");
		usuario.setAtivo(ativo);
		em.persist(usuario);
		return usuario;
	}
}
