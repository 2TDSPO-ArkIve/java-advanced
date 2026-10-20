package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.AnimalResponsavelId;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.Veterinario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ConsultaDiagnosticoRepositoryOracleClobRegressionTest {

	private static final LocalDate DATA_ATUAL = LocalDate.of(2026, 10, 19);

	@Autowired
	TestEntityManager em;

	@Autowired
	ConsultaRepository consultas;

	@Autowired
	DiagnosticoRepository diagnosticos;

	private int sequenciaCrmv;

	@Test
	void consultaDoResponsavelUsaExistsSemDuplicarEPreservaFiltrosEPaginacao() {
		Responsavel responsavel = responsavel("Maria", "doc-maria");
		Animal epona = animal("Epona");
		vinculo(epona, responsavel, DATA_ATUAL.minusDays(30), null, "S");
		vinculo(epona, responsavel, DATA_ATUAL.minusDays(5), null, "S");
		Consulta primeira = consulta(epona, "2026-10-10T10:00:00", "FI", "PRESENCIAL");
		primeira.setObservacaoResponsavel("CLOB entregue pelo responsavel");
		Consulta segunda = consulta(epona, "2026-10-15T10:00:00", "FI", "REMOTA");

		Animal semAcesso = animal("Outro paciente");
		consulta(semAcesso, "2026-10-12T10:00:00", "FI", "PRESENCIAL");
		em.flush();
		em.clear();

		PageRequest primeiraPagina = PageRequest.of(0, 1, Sort.by("id"));
		PageRequest segundaPagina = PageRequest.of(1, 1, Sort.by("id"));
		Page<Consulta> paginaUm = buscarConsultas(responsavel.getId(), null, null, null, null, null, primeiraPagina);
		Page<Consulta> paginaDois = buscarConsultas(responsavel.getId(), null, null, null, null, null, segundaPagina);

		assertEquals(2, paginaUm.getTotalElements());
		assertEquals(2, paginaUm.getTotalPages());
		assertEquals(2, paginaDois.getTotalElements());
		assertEquals(List.of(primeira.getId(), segunda.getId()), List.of(
				paginaUm.getContent().get(0).getId(), paginaDois.getContent().get(0).getId()));

		assertEquals(List.of(segunda.getId()), buscarConsultas(
				responsavel.getId(), segunda.getId(), "pon", epona.getArkivePatientId().toLowerCase(),
				LocalDateTime.parse("2026-10-15T10:00:00"), LocalDateTime.parse("2026-10-15T10:00:00"),
				PageRequest.of(0, 10)).map(Consulta::getId).getContent());
		assertTrue(buscarConsultas(responsavel.getId(), null, "Outro", null, null, null,
				PageRequest.of(0, 10)).isEmpty());
	}

	@Test
	void diagnosticoDoResponsavelUsaExistsSemDistinctSobreClobs() {
		Responsavel responsavel = responsavel("Ana", "doc-ana");
		Animal animal = animal("Bilu");
		vinculo(animal, responsavel, DATA_ATUAL.minusDays(30), null, "S");
		vinculo(animal, responsavel, DATA_ATUAL.minusDays(2), null, "S");
		Consulta consulta = consulta(animal, "2026-10-10T10:00:00", "AP", "PRESENCIAL");
		Diagnostico primeiro = diagnostico(consulta, "Hipotese CLOB 1", "AUSENTE");
		Diagnostico segundo = diagnostico(consulta, "Hipotese CLOB 2", "MODERADA");
		diagnostico(consulta(animal("Sem acesso"), "2026-10-11T10:00:00", "AP", "REMOTA"),
				"Nao autorizado", "GRAVE");
		em.flush();
		em.clear();

		Page<Diagnostico> todos = diagnosticos.buscarParaResponsavel(
				responsavel.getId(), DATA_ATUAL, null, null, null, null,
				PageRequest.of(0, 10, Sort.by("id")));

		assertEquals(2, todos.getTotalElements());
		assertEquals(List.of(primeiro.getId(), segundo.getId()), todos.map(Diagnostico::getId).getContent());
		assertEquals(List.of(segundo.getId()), diagnosticos.buscarParaResponsavel(
				responsavel.getId(), DATA_ATUAL, consulta.getId(), null, "MODERADA", "N",
				PageRequest.of(0, 10)).map(Diagnostico::getId).getContent());
	}

	private Page<Consulta> buscarConsultas(
			Long responsavelId,
			Long consultaId,
			String pacienteNome,
			String arkivePatientId,
			LocalDateTime inicio,
			LocalDateTime fim,
			PageRequest pageable
	) {
		return consultas.buscarParaResponsavel(
				responsavelId, DATA_ATUAL, consultaId, pacienteNome, arkivePatientId, inicio, fim,
				null, null, null, null, null, pageable);
	}

	private Responsavel responsavel(String nome, String documento) {
		Responsavel responsavel = new Responsavel();
		responsavel.setNome(nome);
		responsavel.setDocumento(documento);
		responsavel.setTipo("RESPONSAVEL");
		return em.persist(responsavel);
	}

	private Animal animal(String nome) {
		Especie especie = new Especie();
		especie.setNome(nome + " especie");
		em.persist(especie);
		Animal animal = new Animal();
		animal.setNome(nome);
		animal.setEspecie(especie);
		animal.setObservacaoResponsavelPendente("CLOB pendente");
		return em.persist(animal);
	}

	private Consulta consulta(Animal animal, String dataHora, String status, String modalidade) {
		Veterinario veterinario = new Veterinario();
		veterinario.setNome("Veterinario " + dataHora);
		veterinario.setCrmv("SP" + ++sequenciaCrmv);
		em.persist(veterinario);
		Consulta consulta = new Consulta();
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setDataHora(LocalDateTime.parse(dataHora));
		consulta.setModalidade(modalidade);
		consulta.setMotivo("Motivo CLOB");
		consulta.setStatus(status);
		return em.persist(consulta);
	}

	private Diagnostico diagnostico(Consulta consulta, String texto, String severidade) {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setConsulta(consulta);
		diagnostico.setDiagnostico(texto);
		diagnostico.setSeveridade(severidade);
		diagnostico.setConfirmado("N");
		diagnostico.setInsightIa("Insight CLOB");
		diagnostico.setConfianca(BigDecimal.valueOf(70));
		return em.persist(diagnostico);
	}

	private void vinculo(
			Animal animal,
			Responsavel responsavel,
			LocalDate inicio,
			LocalDate fim,
			String ativo
	) {
		AnimalResponsavelId id = new AnimalResponsavelId();
		id.setAnimalId(animal.getId());
		id.setResponsavelId(responsavel.getId());
		id.setDataInicio(inicio);
		AnimalResponsavel vinculo = new AnimalResponsavel();
		vinculo.setId(id);
		vinculo.setAnimal(animal);
		vinculo.setResponsavel(responsavel);
		vinculo.setTipoVinculo("TUTOR_LEGAL");
		vinculo.setDataFim(fim);
		vinculo.setAtivo(ativo);
		em.persist(vinculo);
	}
}
