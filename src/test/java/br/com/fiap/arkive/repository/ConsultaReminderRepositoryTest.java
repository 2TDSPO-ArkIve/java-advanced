package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Veterinario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ConsultaReminderRepositoryTest {

	@Autowired EntityManager em;
	@Autowired ConsultaRepository consultaRepository;

	@Test
	void buscaSomenteAgendadasFuturasDentroDaJanelaInclusiva() {
		LocalDateTime agora = LocalDateTime.of(2026, 10, 20, 14, 0);
		Especie especie = especie();
		Veterinario veterinario = veterinario();
		Animal animal = animal(especie);
		Consulta dezMinutos = consulta(animal, veterinario, "AG", agora.plusMinutes(10), "dez");
		Consulta trintaMinutos = consulta(animal, veterinario, "AG", agora.plusMinutes(30), "trinta");
		consulta(animal, veterinario, "AG", agora.plusMinutes(31), "fora");
		consulta(animal, veterinario, "AG", agora.minusSeconds(1), "passada");
		consulta(animal, veterinario, "EP", agora.plusMinutes(5), "ep");
		consulta(animal, veterinario, "AP", agora.plusMinutes(5), "ap");
		consulta(animal, veterinario, "FI", agora.plusMinutes(5), "fi");
		consulta(animal, veterinario, "CA", agora.plusMinutes(5), "ca");
		em.flush();
		em.clear();

		List<Consulta> encontradas = consultaRepository.buscarAgendadasParaLembrete(
				agora, agora.plusMinutes(30));

		assertEquals(List.of(dezMinutos.getId(), trintaMinutos.getId()),
				encontradas.stream().map(Consulta::getId).toList());
	}

	private Especie especie() {
		Especie especie = new Especie();
		especie.setNome("Equino-reminder");
		em.persist(especie);
		return especie;
	}

	private Veterinario veterinario() {
		Veterinario veterinario = new Veterinario();
		veterinario.setNome("Dra. Reminder");
		veterinario.setCrmv("SP99999");
		em.persist(veterinario);
		return veterinario;
	}

	private Animal animal(Especie especie) {
		Animal animal = new Animal();
		animal.setNome("Epona");
		animal.setEspecie(especie);
		animal.assignArkivePatientId("ARK-PET-REMINDER-TEST-00001");
		em.persist(animal);
		return animal;
	}

	private Consulta consulta(Animal animal, Veterinario veterinario, String status,
			LocalDateTime dataHora, String motivo) {
		Consulta consulta = new Consulta();
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setStatus(status);
		consulta.setDataHora(dataHora);
		consulta.setModalidade("PRESENCIAL");
		consulta.setMotivo(motivo);
		em.persist(consulta);
		return consulta;
	}
}
