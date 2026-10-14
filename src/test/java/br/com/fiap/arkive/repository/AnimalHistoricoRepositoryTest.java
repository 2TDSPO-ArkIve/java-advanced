package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.Veterinario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
class AnimalHistoricoRepositoryTest {
	@Autowired TestEntityManager em;
	@Autowired ConsultaRepository consultas;
	@Autowired DiagnosticoRepository diagnosticos;
	@Autowired PrescricaoRepository prescricoes;
	private int crmv;

	@Test
	void historicoRestringeAnimalOrdenaDataEIdDescEMantemConsultasSemClinica() {
		Animal animal = animal("Paciente");
		Consulta antiga = consulta(animal, "2025-01-01T09:00:00", "FI");
		Consulta recente = consulta(animal, "2026-01-01T09:00:00", "FI");
		Consulta empate = consulta(animal, "2026-01-01T09:00:00", "CA");
		Consulta futura = consulta(animal, "2027-01-01T09:00:00", "AG");
		Consulta outroPaciente = consulta(animal("Outro paciente"), "2028-01-01T09:00:00", "FI");
		em.flush(); em.clear();
		var resultado = consultas.buscarHistoricoPorAnimal(animal.getId());
		assertEquals(List.of(futura.getId(), empate.getId(), recente.getId(), antiga.getId()),
				resultado.stream().map(Consulta::getId).toList());
		assertTrue(resultado.stream().allMatch(c -> c.getClinica() == null));
		assertTrue(resultado.stream().noneMatch(c -> c.getId().equals(outroPaciente.getId())));
		assertEquals("Vet", resultado.get(0).getVeterinario().getNome());
		assertTrue(consultas.buscarHistoricoPorAnimal(-1L).isEmpty());
	}

	@Test
	void diagnosticoConfirmadoNaoDependeDeValidacaoDeInsightIa() {
		Consulta consulta = consulta(animal("Paciente"), "2026-01-01T09:00:00", "FI");
		diagnostico(consulta, "Anterior aprovado", "S", null);
		Diagnostico aprovado = diagnostico(consulta, "Diagnostico final", "S", null);
		diagnostico(consulta, "SECRET_AI", "N", null);
		diagnostico(consulta, "SECRET_SEM_CONFIRMACAO", "N", "S");
		Consulta outra = consulta(animal("Outro"), "2026-01-01T09:00:00", "FI");
		diagnostico(outra, "SECRET_OUTRO_PACIENTE", "S", null);
		em.flush(); em.clear();
		var resultado = diagnosticos.buscarDiagnosticosConfirmadosVeterinario(consulta.getId(), PageRequest.of(0, 1));
		assertEquals(1, resultado.size());
		assertEquals(aprovado.getId(), resultado.get(0).getId());
		assertEquals("Diagnostico final", resultado.get(0).getDiagnostico());
	}

	@Test
	void suporteClinicoIaContinuaDisponivelAntesEDepoisDaAvaliacao() {
		Consulta consulta = consulta(animal("Paciente"), "2026-01-01T09:00:00", "AP");
		Diagnostico naoAvaliado = suporteIa(consulta, "Nao avaliado", null);
		Diagnostico util = suporteIa(consulta, "Util", "S");
		Diagnostico naoUtil = suporteIa(consulta, "Nao util", "N");
		diagnostico(consulta, "Manual", "N", null);
		em.flush(); em.clear();

		var resultado = diagnosticos.buscarSuportesClinicos(consulta.getId(), PageRequest.of(0, 10));

		assertEquals(List.of(naoUtil.getId(), util.getId(), naoAvaliado.getId()),
				resultado.stream().map(Diagnostico::getId).toList());
	}

	@Test
	void custosConsideramSomenteConsultasFinalizadasEAgrupamPrescricoes() {
		Animal animal = animal("Paciente");
		Consulta antiga = consulta(animal, "2026-01-01T09:00:00", "FI");
		antiga.setValorConsulta(new BigDecimal("120.00"));
		Consulta recente = consulta(animal, "2026-02-01T09:00:00", "FI");
		Consulta emProgresso = consulta(animal, "2026-03-01T09:00:00", "EP");
		prescricao(antiga, "10.25");
		prescricao(antiga, "20.25");
		prescricao(antiga, null);
		prescricao(emProgresso, "999.00");
		em.flush();
		em.clear();

		var consultasComCusto = consultas.buscarCustosFinalizadosPorAnimal(animal.getId());
		var agregados = prescricoes.somarCustosMedicamentosFinalizadosPorAnimal(animal.getId());

		assertEquals(List.of(recente.getId(), antiga.getId()),
				consultasComCusto.stream().map(Consulta::getId).toList());
		assertEquals(1, agregados.size());
		assertEquals(antiga.getId(), agregados.get(0).getConsultaId());
		assertEquals(new BigDecimal("30.50"), agregados.get(0).getValorMedicamentos());
		assertEquals(2L, agregados.get(0).getQuantidadePrescricoesComValor());
	}

	@Test
	void consultasEAgregadosFinanceirosPodemSerEscopadosPorVeterinarioNoBanco() {
		Animal animal = animal("Paciente compartilhado");
		Consulta consultaVetA = consulta(animal, "2026-10-20T14:30:00", "FI");
		consultaVetA.setValorConsulta(new BigDecimal("100.00"));
		prescricao(consultaVetA, "40.00");
		Consulta consultaVetB = consulta(animal, "2026-10-19T14:30:00", "FI");
		consultaVetB.setValorConsulta(new BigDecimal("200.00"));
		prescricao(consultaVetB, "80.00");
		Consulta naoFinalizadaVetA = consulta(animal, "2026-10-21T14:30:00", "EP");
		naoFinalizadaVetA.setVeterinario(consultaVetA.getVeterinario());
		naoFinalizadaVetA.setValorConsulta(new BigDecimal("999.00"));
		prescricao(naoFinalizadaVetA, "999.00");
		em.flush();
		em.clear();

		Long veterinarioAId = consultaVetA.getVeterinario().getId();
		var consultasDoVetA = consultas.buscarCustosFinalizadosPorAnimalEVeterinario(
				animal.getId(), veterinarioAId);
		var dashboardVetA = consultas.buscarCustosFinalizadosPorVeterinario(veterinarioAId);
		var medicamentosPacienteVetA = prescricoes.somarCustosMedicamentosFinalizadosPorAnimalEVeterinario(
				animal.getId(), veterinarioAId);
		var medicamentosDashboardVetA = prescricoes.somarCustosMedicamentosFinalizadosPorVeterinario(veterinarioAId);

		assertEquals(List.of(consultaVetA.getId()), consultasDoVetA.stream().map(Consulta::getId).toList());
		assertEquals(List.of(consultaVetA.getId()), dashboardVetA.stream().map(Consulta::getId).toList());
		assertEquals(1, medicamentosPacienteVetA.size());
		assertEquals(consultaVetA.getId(), medicamentosPacienteVetA.get(0).getConsultaId());
		assertEquals(new BigDecimal("40.00"), medicamentosPacienteVetA.get(0).getValorMedicamentos());
		assertEquals(1, medicamentosDashboardVetA.size());
		assertEquals(new BigDecimal("40.00"), medicamentosDashboardVetA.get(0).getValorMedicamentos());

		assertEquals(2, consultas.buscarCustosFinalizadosPorAnimal(animal.getId()).size(),
				"A visão autorizada do responsável continua abrangendo os dois veterinários.");
	}

	private Animal animal(String nome) {
		Especie especie = new Especie(); especie.setNome(nome + " especie"); em.persist(especie);
		Animal animal = new Animal(); animal.setNome(nome); animal.setEspecie(especie);
		return em.persist(animal);
	}

	private Consulta consulta(Animal animal, String data, String status) {
		Veterinario vet = new Veterinario(); vet.setNome("Vet"); vet.setCrmv("SP" + ++crmv); em.persist(vet);
		Consulta consulta = new Consulta(); consulta.setAnimal(animal); consulta.setVeterinario(vet);
		consulta.setDataHora(LocalDateTime.parse(data)); consulta.setModalidade("REMOTA");
		consulta.setMotivo("Retorno"); consulta.setStatus(status);
		return em.persist(consulta);
	}

	private Diagnostico diagnostico(Consulta consulta, String texto, String confirmado, String validacao) {
		Diagnostico diagnostico = new Diagnostico(); diagnostico.setConsulta(consulta); diagnostico.setDiagnostico(texto);
		diagnostico.setConfirmado(confirmado); diagnostico.setValidacaoVet(validacao);
		return em.persist(diagnostico);
	}

	private Diagnostico suporteIa(Consulta consulta, String texto, String validacao) {
		Diagnostico diagnostico = diagnostico(consulta, texto, "N", validacao);
		diagnostico.setInsightIa("Insight " + texto);
		diagnostico.setConfianca(BigDecimal.valueOf(70));
		return diagnostico;
	}

	private Prescricao prescricao(Consulta consulta, String valorMedicamento) {
		Prescricao prescricao = new Prescricao();
		prescricao.setConsulta(consulta);
		prescricao.setMedicamento("Medicamento");
		prescricao.setDosagem("1 comprimido");
		prescricao.setDataInicio(LocalDate.parse("2026-01-01"));
		prescricao.setValorMedicamento(valorMedicamento == null ? null : new BigDecimal(valorMedicamento));
		return em.persist(prescricao);
	}
}
