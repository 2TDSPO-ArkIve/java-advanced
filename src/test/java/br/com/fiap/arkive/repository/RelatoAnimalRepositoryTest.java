package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.RelatoAnimal;
import br.com.fiap.arkive.entity.Responsavel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class RelatoAnimalRepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private RelatoAnimalRepository repository;

	@Test
	void persisteMultiplosRelatosEPaginaIdsPorAnimalEmOrdemEstavel() {
		Animal animal = animal("Epona");
		Animal outroAnimal = animal("Outro");
		Responsavel maria = responsavel("Maria", "11111111111");
		Responsavel joana = responsavel("Joana", "22222222222");
		RelatoAnimal antigo = relato(animal, maria, "Antigo", LocalDateTime.of(2026, 10, 15, 9, 0));
		RelatoAnimal recenteMenorId = relato(animal, maria, "Recente A", LocalDateTime.of(2026, 10, 16, 9, 0));
		RelatoAnimal recenteMaiorId = relato(animal, joana, "Recente B", LocalDateTime.of(2026, 10, 16, 9, 0));
		relato(outroAnimal, maria, "Outro paciente", LocalDateTime.of(2026, 10, 17, 9, 0));
		em.flush();
		em.clear();

		var pagina = repository.listarIdsPorAnimal(animal.getId(), PageRequest.of(0, 2));

		assertEquals(3, pagina.getTotalElements());
		assertEquals(List.of(recenteMaiorId.getId(), recenteMenorId.getId()), pagina.getContent());
		var detalhes = repository.buscarDetalhesPorIds(pagina.getContent());
		assertEquals(2, detalhes.size());
		assertEquals(List.of("Joana", "Maria"), detalhes.stream()
				.map(relato -> relato.getAutor().getNome()).sorted().toList());
		assertEquals(antigo.getAnimal().getId(), animal.getId());
	}

	private Animal animal(String nome) {
		Especie especie = new Especie();
		especie.setNome(nome + " especie");
		em.persist(especie);
		Animal animal = new Animal();
		animal.setNome(nome);
		animal.setEspecie(especie);
		return em.persist(animal);
	}

	private Responsavel responsavel(String nome, String documento) {
		Responsavel responsavel = new Responsavel();
		responsavel.setNome(nome);
		responsavel.setDocumento(documento);
		responsavel.setTipo("RESPONSAVEL");
		return em.persist(responsavel);
	}

	private RelatoAnimal relato(Animal animal, Responsavel autor, String texto, LocalDateTime dataCriacao) {
		RelatoAnimal relato = new RelatoAnimal();
		relato.setAnimal(animal);
		relato.setAutor(autor);
		relato.setTexto(texto);
		relato.setTextoBusca(texto.toUpperCase());
		relato.setDataCriacao(dataCriacao);
		relato.setOrigemTranscricao("N");
		return em.persist(relato);
	}
}
