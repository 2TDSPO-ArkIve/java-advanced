package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository.CustoMedicamentoPorConsulta;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VeterinarioCustoServiceTest {

	private VeterinarioRepository veterinarios;
	private ConsultaRepository consultas;
	private PrescricaoRepository prescricoes;
	private VeterinarioCustoService service;

	@BeforeEach
	void setUp() {
		veterinarios = mock(VeterinarioRepository.class);
		consultas = mock(ConsultaRepository.class);
		prescricoes = mock(PrescricaoRepository.class);
		service = new VeterinarioCustoService(veterinarios, consultas, prescricoes);
	}

	@Test
	void agregaSomenteConsultasFinalizadasDoVeterinarioAutenticado() {
		Veterinario veterinario = veterinario(10L, "Dra. Fernanda");
		Veterinario outroVeterinario = veterinario(11L, "Dr. Bruno");
		Consulta primeira = consulta(42L, veterinario, "Epona", "ARK-PET-EPONA", "2026-10-20T14:30:00", "100.00");
		Consulta segunda = consulta(41L, veterinario, "Nina", "ARK-PET-NINA", "2026-10-10T09:00:00", "0.00");
		Consulta consultaOutroVeterinario = consulta(43L, outroVeterinario, "Epona", "ARK-PET-EPONA",
				"2026-10-19T14:30:00", "200.00");
		when(veterinarios.findById(10L)).thenReturn(Optional.of(veterinario));
		when(veterinarios.findById(11L)).thenReturn(Optional.of(outroVeterinario));
		when(consultas.buscarCustosFinalizadosPorVeterinario(10L)).thenReturn(List.of(primeira, segunda));
		when(consultas.buscarCustosFinalizadosPorVeterinario(11L)).thenReturn(List.of(consultaOutroVeterinario));
		when(prescricoes.somarCustosMedicamentosFinalizadosPorVeterinario(10L)).thenReturn(List.of(
				agregado(42L, "40.00", 1L), agregado(41L, "0.00", 1L)));
		when(prescricoes.somarCustosMedicamentosFinalizadosPorVeterinario(11L)).thenReturn(List.of(
				agregado(43L, "80.00", 1L)));

		var response = service.buscarCustosAutenticados(veterinarioPrincipal(10L));

		assertEquals(10L, response.veterinarioId());
		assertEquals("Dra. Fernanda", response.veterinarioNome());
		assertEquals(new BigDecimal("100.00"), response.totalConsultas());
		assertEquals(new BigDecimal("40.00"), response.totalMedicamentos());
		assertEquals(new BigDecimal("140.00"), response.totalGeral());
		assertEquals(2, response.quantidadeConsultasComValor());
		assertEquals(2, response.quantidadePrescricoesComValor());
		assertEquals(List.of(42L, 41L), response.consultas().stream().map(c -> c.consultaId()).toList());
		assertEquals("ARK-PET-EPONA", response.consultas().get(0).animalArkivePatientId());
		assertEquals(new BigDecimal("0.00"), response.consultas().get(1).valorConsulta());
		assertEquals(new BigDecimal("280.00"),
				service.buscarCustosAutenticados(veterinarioPrincipal(11L)).totalGeral());
	}

	@Test
	void valorAusentePermaneceNuloNoDetalheEContaComoZeroNoTotal() {
		Veterinario veterinario = veterinario(10L, "Dra. Fernanda");
		Consulta consulta = consulta(42L, veterinario, "Epona", "ARK-PET-EPONA", "2026-10-20T14:30:00", null);
		when(veterinarios.findById(10L)).thenReturn(Optional.of(veterinario));
		when(consultas.buscarCustosFinalizadosPorVeterinario(10L)).thenReturn(List.of(consulta));
		when(prescricoes.somarCustosMedicamentosFinalizadosPorVeterinario(10L)).thenReturn(List.of());

		var response = service.buscarCustosAutenticados(veterinarioPrincipal(10L));

		assertNull(response.consultas().get(0).valorConsulta());
		assertEquals(new BigDecimal("0.00"), response.totalGeral());
		assertEquals(0, response.quantidadeConsultasComValor());
		assertEquals(0, response.quantidadePrescricoesComValor());
	}

	@Test
	void identidadeDoDashboardVemExclusivamenteDoPrincipal() {
		Veterinario veterinario = veterinario(22L, "Dr. Bruno");
		when(veterinarios.findById(22L)).thenReturn(Optional.of(veterinario));
		when(consultas.buscarCustosFinalizadosPorVeterinario(22L)).thenReturn(List.of());

		service.buscarCustosAutenticados(veterinarioPrincipal(22L));

		verify(consultas).buscarCustosFinalizadosPorVeterinario(22L);
		verify(consultas, never()).buscarCustosFinalizadosPorVeterinario(10L);
	}

	@Test
	void falhaFechadoParaPapelIncorretoPerfilSemIdOuVinculoInvalido() {
		assertThrows(AccessDeniedException.class,
				() -> service.buscarCustosAutenticados(principal(TipoUsuario.RESPONSAVEL, 40L, null)));
		assertThrows(AccessDeniedException.class,
				() -> service.buscarCustosAutenticados(principal(TipoUsuario.VETERINARIO, null, null)));
		assertThrows(AccessDeniedException.class,
				() -> service.buscarCustosAutenticados(veterinarioPrincipal(99L)));
		verify(consultas, never()).buscarCustosFinalizadosPorVeterinario(99L);
	}

	private Consulta consulta(Long id, Veterinario veterinario, String animalNome, String arkiveId,
			String dataHora, String valor) {
		Animal animal = new Animal();
		animal.setId(id + 100);
		animal.setNome(animalNome);
		animal.assignArkivePatientId(arkiveId);
		Consulta consulta = new Consulta();
		consulta.setId(id);
		consulta.setVeterinario(veterinario);
		consulta.setAnimal(animal);
		consulta.setStatus("FI");
		consulta.setDataHora(LocalDateTime.parse(dataHora));
		consulta.setValorConsulta(valor == null ? null : new BigDecimal(valor));
		return consulta;
	}

	private Veterinario veterinario(Long id, String nome) {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(id);
		veterinario.setNome(nome);
		return veterinario;
	}

	private CustoMedicamentoPorConsulta agregado(Long consultaId, String valor, Long quantidade) {
		return new CustoMedicamentoPorConsulta() {
			@Override public Long getConsultaId() { return consultaId; }
			@Override public BigDecimal getValorMedicamentos() { return new BigDecimal(valor); }
			@Override public Long getQuantidadePrescricoesComValor() { return quantidade; }
		};
	}

	private UsuarioPrincipal veterinarioPrincipal(Long veterinarioId) {
		return principal(TipoUsuario.VETERINARIO, null, veterinarioId);
	}

	private UsuarioPrincipal principal(TipoUsuario tipo, Long responsavelId, Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "hash", tipo,
				"S", false, responsavelId, veterinarioId, null);
	}
}
