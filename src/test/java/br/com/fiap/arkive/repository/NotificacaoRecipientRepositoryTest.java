package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.AnimalResponsavelId;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class NotificacaoRecipientRepositoryTest {

	@Autowired TestEntityManager em;
	@Autowired UsuarioRepository usuarioRepository;

	@Test
	void selecionaTodosESomenteResponsaveisAtivosComVinculoAtivoEVigente() {
		LocalDate hoje = LocalDate.of(2026, 10, 9);
		Animal animal = animal();
		Usuario atualUm = conta(vinculo(animal, "Atual 1", "doc-1", hoje.minusYears(1), null, "S"),
				TipoUsuario.RESPONSAVEL, "S", "atual1@example.test");
		Usuario atualDois = conta(vinculo(animal, "Atual 2", "doc-2", hoje, hoje, "S"),
				TipoUsuario.RESPONSAVEL, "S", "atual2@example.test");
		conta(vinculo(animal, "Inativo", "doc-3", hoje.minusDays(1), null, "N"),
				TipoUsuario.RESPONSAVEL, "S", "rel-inativa@example.test");
		conta(vinculo(animal, "Historico", "doc-4", hoje.minusYears(1), hoje.minusDays(1), "S"),
				TipoUsuario.RESPONSAVEL, "S", "historico@example.test");
		conta(vinculo(animal, "Futuro", "doc-5", hoje.plusDays(1), null, "S"),
				TipoUsuario.RESPONSAVEL, "S", "futuro@example.test");
		conta(vinculo(animal, "Conta inativa", "doc-6", hoje.minusDays(1), null, "S"),
				TipoUsuario.RESPONSAVEL, "N", "inativo@example.test");
		conta(vinculo(animal, "Papel errado", "doc-7", hoje.minusDays(1), null, "S"),
				TipoUsuario.VETERINARIO, "S", "vet@example.test");
		vinculo(animal, "Sem conta", "doc-8", hoje.minusDays(1), null, "S");
		em.flush();
		em.clear();

		List<Usuario> encontrados = usuarioRepository.findResponsaveisAtivosVigentesDoAnimal(animal.getId(), hoje);

		assertEquals(Set.of(atualUm.getId(), atualDois.getId()),
				encontrados.stream().map(Usuario::getId).collect(Collectors.toSet()));
	}

	private Animal animal() {
		Especie especie = new Especie();
		especie.setNome("Canino-notificacao");
		em.persist(especie);
		Animal animal = new Animal();
		animal.setNome("Rex");
		animal.setEspecie(especie);
		return em.persist(animal);
	}

	private Responsavel vinculo(Animal animal, String nome, String documento, LocalDate inicio,
			LocalDate fim, String ativo) {
		Responsavel responsavel = new Responsavel();
		responsavel.setNome(nome);
		responsavel.setDocumento(documento);
		responsavel.setTipo("CUIDADOR");
		em.persist(responsavel);
		AnimalResponsavelId id = new AnimalResponsavelId();
		id.setAnimalId(animal.getId());
		id.setResponsavelId(responsavel.getId());
		id.setDataInicio(inicio);
		AnimalResponsavel vinculo = new AnimalResponsavel();
		vinculo.setId(id);
		vinculo.setAnimal(animal);
		vinculo.setResponsavel(responsavel);
		vinculo.setTipoVinculo("CUIDADOR");
		vinculo.setDataFim(fim);
		vinculo.setAtivo(ativo);
		em.persist(vinculo);
		return responsavel;
	}

	private Usuario conta(Responsavel responsavel, TipoUsuario tipo, String ativo, String login) {
		Usuario usuario = new Usuario();
		usuario.setNome(responsavel.getNome());
		usuario.setTipo(tipo);
		usuario.setResponsavel(responsavel);
		usuario.setLogin(login);
		usuario.setSenhaHash("hash");
		usuario.setAtivo(ativo);
		return em.persist(usuario);
	}
}
