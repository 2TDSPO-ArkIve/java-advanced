package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.AnimalCustoResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository.CustoMedicamentoPorConsulta;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimalCustoServiceTest {

	private ConsultaRepository consultaRepository;
	private PrescricaoRepository prescricaoRepository;
	private ClinicalAccessService clinicalAccessService;
	private AnimalCustoService service;
	private Animal animal;
	private UsuarioPrincipal principal;

	@BeforeEach
	void setUp() {
		AnimalRepository animalRepository = mock(AnimalRepository.class);
		consultaRepository = mock(ConsultaRepository.class);
		prescricaoRepository = mock(PrescricaoRepository.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		service = new AnimalCustoService(
				animalRepository, consultaRepository, prescricaoRepository, clinicalAccessService,
				new FinancialVisibilityService());
		animal = new Animal();
		animal.setId(50L);
		animal.setNome("Epona");
		animal.assignArkivePatientId("ARK-PET-EPONA");
		principal = new UsuarioPrincipal(1L, "Responsável", "responsavel@arkive.com", "hash",
				TipoUsuario.RESPONSAVEL, "S", false, 40L, null, null);
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal));
	}

	@Test
	void semCustosRetornaTotaisZeroEHistoricoNuloPermaneceNulo() {
		Consulta consulta = consulta(42L, "2026-10-12T14:30:00", "FI", null);
		when(consultaRepository.buscarCustosFinalizadosPorAnimal(50L)).thenReturn(List.of(consulta));
		when(clinicalAccessService.podeLerConsulta(principal, consulta)).thenReturn(true);
		when(prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(50L)).thenReturn(List.of());

		AnimalCustoResponse response = service.buscar(50L, principal);

		assertEquals(new BigDecimal("0.00"), response.totalConsultas());
		assertEquals(new BigDecimal("0.00"), response.totalMedicamentos());
		assertEquals(new BigDecimal("0.00"), response.totalGeral());
		assertEquals(0, response.quantidadeConsultasComValor());
		assertEquals(0, response.quantidadePrescricoesComValor());
		assertNull(response.consultas().get(0).valorConsulta());
		assertEquals(new BigDecimal("0.00"), response.consultas().get(0).valorMedicamentos());
	}

	@Test
	void agregaConsultaEMultiplasPrescricoesSemMisturarConceitos() {
		Consulta consulta = consulta(42L, "2026-10-12T14:30:00", "FI", "180.00");
		when(consultaRepository.buscarCustosFinalizadosPorAnimal(50L)).thenReturn(List.of(consulta));
		when(clinicalAccessService.podeLerConsulta(principal, consulta)).thenReturn(true);
		when(prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(50L))
				.thenReturn(List.of(agregado(42L, "95.50", 2L)));

		AnimalCustoResponse response = service.buscar(50L, principal);

		assertEquals(new BigDecimal("180.00"), response.totalConsultas());
		assertEquals(new BigDecimal("95.50"), response.totalMedicamentos());
		assertEquals(new BigDecimal("275.50"), response.totalGeral());
		assertEquals(1, response.quantidadeConsultasComValor());
		assertEquals(2, response.quantidadePrescricoesComValor());
		assertEquals(new BigDecimal("275.50"), response.consultas().get(0).valorTotal());
	}

	@Test
	void agregaSomenteValorDaConsulta() {
		Consulta consulta = consulta(42L, "2026-10-12T14:30:00", "FI", "180.00");
		when(consultaRepository.buscarCustosFinalizadosPorAnimal(50L)).thenReturn(List.of(consulta));
		when(clinicalAccessService.podeLerConsulta(principal, consulta)).thenReturn(true);
		when(prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(50L)).thenReturn(List.of());

		AnimalCustoResponse response = service.buscar(50L, principal);

		assertEquals(new BigDecimal("180.00"), response.totalConsultas());
		assertEquals(new BigDecimal("0.00"), response.totalMedicamentos());
		assertEquals(new BigDecimal("180.00"), response.totalGeral());
	}

	@Test
	void agregaSomenteValorDeMedicamento() {
		Consulta consulta = consulta(42L, "2026-10-12T14:30:00", "FI", null);
		when(consultaRepository.buscarCustosFinalizadosPorAnimal(50L)).thenReturn(List.of(consulta));
		when(clinicalAccessService.podeLerConsulta(principal, consulta)).thenReturn(true);
		when(prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(50L))
				.thenReturn(List.of(agregado(42L, "25.75", 1L)));

		AnimalCustoResponse response = service.buscar(50L, principal);

		assertEquals(new BigDecimal("0.00"), response.totalConsultas());
		assertEquals(new BigDecimal("25.75"), response.totalMedicamentos());
		assertEquals(new BigDecimal("25.75"), response.totalGeral());
		assertNull(response.consultas().get(0).valorConsulta());
	}

	@Test
	void agregaMultiplasConsultasIgnoraNulosEOrdenaMaisRecentesPrimeiro() {
		Consulta antiga = consulta(41L, "2026-09-01T09:00:00", "FI", "120.10");
		Consulta recente = consulta(42L, "2026-10-12T14:30:00", "FI", null);
		when(consultaRepository.buscarCustosFinalizadosPorAnimal(50L)).thenReturn(List.of(antiga, recente));
		when(clinicalAccessService.podeLerConsulta(principal, antiga)).thenReturn(true);
		when(clinicalAccessService.podeLerConsulta(principal, recente)).thenReturn(true);
		when(prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(50L)).thenReturn(List.of(
				agregado(41L, null, 0L),
				agregado(42L, "30.40", 1L)
		));

		AnimalCustoResponse response = service.buscar(50L, principal);

		assertEquals(List.of(42L, 41L), response.consultas().stream()
				.map(AnimalCustoResponse.ConsultaCusto::consultaId).toList());
		assertEquals(new BigDecimal("120.10"), response.totalConsultas());
		assertEquals(new BigDecimal("30.40"), response.totalMedicamentos());
		assertEquals(new BigDecimal("150.50"), response.totalGeral());
	}

	@Test
	void zeroRegistradoContaComoValorInformado() {
		Consulta consulta = consulta(42L, "2026-10-12T14:30:00", "FI", "0");
		when(consultaRepository.buscarCustosFinalizadosPorAnimal(50L)).thenReturn(List.of(consulta));
		when(clinicalAccessService.podeLerConsulta(principal, consulta)).thenReturn(true);
		when(prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(50L))
				.thenReturn(List.of(agregado(42L, "0", 1L)));

		AnimalCustoResponse response = service.buscar(50L, principal);

		assertEquals(new BigDecimal("0.00"), response.totalGeral());
		assertEquals(1, response.quantidadeConsultasComValor());
		assertEquals(1, response.quantidadePrescricoesComValor());
	}

	@Test
	void somenteFiEConsultasVisiveisContribuemAosTotais() {
		Consulta finalizadaVisivel = consulta(40L, "2026-08-01T09:00:00", "FI", "10.00");
		Consulta finalizadaOculta = consulta(41L, "2026-09-01T09:00:00", "FI", "200.00");
		Consulta emProgresso = consulta(42L, "2026-10-12T14:30:00", "EP", "300.00");
		when(consultaRepository.buscarCustosFinalizadosPorAnimal(50L))
				.thenReturn(List.of(emProgresso, finalizadaOculta, finalizadaVisivel));
		when(clinicalAccessService.podeLerConsulta(principal, finalizadaVisivel)).thenReturn(true);
		when(clinicalAccessService.podeLerConsulta(principal, finalizadaOculta)).thenReturn(false);
		when(prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(50L)).thenReturn(List.of(
				agregado(40L, "5.00", 1L),
				agregado(41L, "50.00", 1L),
				agregado(42L, "70.00", 1L)
		));

		AnimalCustoResponse response = service.buscar(50L, principal);

		assertEquals(List.of(40L), response.consultas().stream()
				.map(AnimalCustoResponse.ConsultaCusto::consultaId).toList());
		assertEquals(new BigDecimal("15.00"), response.totalGeral());
	}

	@Test
	void reutilizaAutorizacaoDeLeituraDoAnimal() {
		service.buscar(50L, principal);

		verify(clinicalAccessService).exigirLeituraAnimal(principal, animal);
	}

	@Test
	void veterinarioUsaAgregacaoRestritaAsPropriasConsultas() {
		UsuarioPrincipal veterinario = new UsuarioPrincipal(2L, "Dra", "vet@arkive.com", "hash",
				TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
		Consulta propria = consulta(42L, "2026-10-12T14:30:00", "FI", "100.00");
		when(consultaRepository.buscarCustosFinalizadosPorAnimalEVeterinario(50L, 10L))
				.thenReturn(List.of(propria));
		when(clinicalAccessService.podeLerConsulta(veterinario, propria)).thenReturn(true);
		when(prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimalEVeterinario(50L, 10L))
				.thenReturn(List.of(agregado(42L, "40.00", 1L)));

		AnimalCustoResponse response = service.buscar(50L, veterinario);

		assertEquals(new BigDecimal("140.00"), response.totalGeral());
		assertEquals(List.of(42L), response.consultas().stream()
				.map(AnimalCustoResponse.ConsultaCusto::consultaId).toList());
		verify(consultaRepository, never()).buscarCustosFinalizadosPorAnimal(50L);
		verify(prescricaoRepository, never()).somarCustosMedicamentosFinalizadosPorAnimal(50L);
	}

	@Test
	void responsavelAutorizadoMantemVisaoFinanceiraCompletaDoPaciente() {
		Consulta consultaVetA = consulta(41L, "2026-10-12T14:30:00", "FI", "100.00");
		Consulta consultaVetB = consulta(42L, "2026-10-13T14:30:00", "FI", "200.00");
		consultaVetB.getVeterinario().setId(11L);
		when(consultaRepository.buscarCustosFinalizadosPorAnimal(50L))
				.thenReturn(List.of(consultaVetB, consultaVetA));
		when(clinicalAccessService.podeLerConsulta(principal, consultaVetA)).thenReturn(true);
		when(clinicalAccessService.podeLerConsulta(principal, consultaVetB)).thenReturn(true);
		when(prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(50L)).thenReturn(List.of(
				agregado(41L, "40.00", 1L), agregado(42L, "80.00", 1L)));

		AnimalCustoResponse response = service.buscar(50L, principal);

		assertEquals(new BigDecimal("300.00"), response.totalConsultas());
		assertEquals(new BigDecimal("120.00"), response.totalMedicamentos());
		assertEquals(new BigDecimal("420.00"), response.totalGeral());
		assertEquals(2, response.consultas().size());
	}

	private Consulta consulta(Long id, String dataHora, String status, String valor) {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(10L);
		Consulta consulta = new Consulta();
		consulta.setId(id);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setDataHora(LocalDateTime.parse(dataHora));
		consulta.setStatus(status);
		consulta.setValorConsulta(valor == null ? null : new BigDecimal(valor));
		return consulta;
	}

	private CustoMedicamentoPorConsulta agregado(Long consultaId, String valor, Long quantidade) {
		return new CustoMedicamentoPorConsulta() {
			@Override
			public Long getConsultaId() {
				return consultaId;
			}

			@Override
			public BigDecimal getValorMedicamentos() {
				return valor == null ? null : new BigDecimal(valor);
			}

			@Override
			public Long getQuantidadePrescricoesComValor() {
				return quantidade;
			}
		};
	}
}
