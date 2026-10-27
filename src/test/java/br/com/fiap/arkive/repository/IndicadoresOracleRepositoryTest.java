package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.exception.IndicadoresDatabaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndicadoresOracleRepositoryTest {

	private SimpleJdbcCall totalConsultas;
	private SimpleJdbcCall totalFinalizadas;
	private SimpleJdbcCall totalPacientesAtivos;
	private IndicadoresOracleRepository repository;

	@BeforeEach
	void setUp() {
		totalConsultas = mock(SimpleJdbcCall.class);
		totalFinalizadas = mock(SimpleJdbcCall.class);
		totalPacientesAtivos = mock(SimpleJdbcCall.class);
		repository = new IndicadoresOracleRepository(
				totalConsultas, totalFinalizadas, totalPacientesAtivos);
	}

	@Test
	void enviaParametroNomeadoEConverteNumberDecimal() {
		when(totalConsultas.execute(any(SqlParameterSource.class)))
				.thenReturn(Map.of("P_TOTAL", new BigDecimal("7")));

		long total = repository.totalConsultasAnimal(15L);

		assertEquals(7L, total);
		ArgumentCaptor<SqlParameterSource> parametros =
				ArgumentCaptor.forClass(SqlParameterSource.class);
		verify(totalConsultas).execute(parametros.capture());
		assertEquals(15L, parametros.getValue().getValue("P_ID_ANIMAL"));
		assertEquals(Types.NUMERIC, parametros.getValue().getSqlType("P_ID_ANIMAL"));
	}

	@Test
	void usaProcedureEspecificaParaConsultasFinalizadas() {
		when(totalFinalizadas.execute(any(SqlParameterSource.class)))
				.thenReturn(Map.of("P_TOTAL", BigDecimal.ONE));

		assertEquals(1L, repository.totalConsultasFinalizadas(1L));

		verify(totalFinalizadas).execute(any(SqlParameterSource.class));
		verify(totalConsultas, never()).execute(any(SqlParameterSource.class));
	}

	@Test
	void converteOutNumberInteiroDoTotalGlobal() {
		when(totalPacientesAtivos.execute()).thenReturn(Map.of("P_TOTAL", 52));

		assertEquals(52L, repository.totalPacientesAtivos());
	}

	@Test
	void falhaOracleViraErroSeguroSemDetalhesSensiveis() {
		when(totalPacientesAtivos.execute()).thenThrow(
				new DataAccessResourceFailureException("ORA-01017 usuario e senha invalidos"));

		IndicadoresDatabaseException exception = assertThrows(
				IndicadoresDatabaseException.class, repository::totalPacientesAtivos);

		assertEquals("Nao foi possivel consultar os indicadores no Oracle.", exception.getMessage());
		assertFalse(exception.getMessage().contains("ORA-01017"));
	}

	@Test
	void outAusenteEhTratadoComoFalhaDeIntegracao() {
		when(totalPacientesAtivos.execute()).thenReturn(Map.of());

		assertThrows(IndicadoresDatabaseException.class, repository::totalPacientesAtivos);
	}

	@Test
	void numberFracionarioNaoEhTruncadoSilenciosamente() {
		when(totalPacientesAtivos.execute()).thenReturn(Map.of("P_TOTAL", new BigDecimal("1.5")));

		assertThrows(IndicadoresDatabaseException.class, repository::totalPacientesAtivos);
	}
}
