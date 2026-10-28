package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.exception.IndicadoresDatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Repository
@Profile("!local-nodb")
public class IndicadoresOracleRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndicadoresOracleRepository.class);
	private static final String PACKAGE_NAME = "PKG_ARKIVE_INDICADORES";
	private static final String PARAM_ANIMAL_ID = "P_ID_ANIMAL";
	private static final String PARAM_TOTAL = "P_TOTAL";
	private static final Pattern ORACLE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_$#]{0,127}");

	private final SimpleJdbcCall totalConsultasAnimalCall;
	private final SimpleJdbcCall totalConsultasFinalizadasCall;
	private final SimpleJdbcCall totalPacientesAtivosCall;

	@Autowired
	public IndicadoresOracleRepository(
			JdbcTemplate jdbcTemplate,
			@Value("${arkive.oracle.indicadores.schema:RM561996}") String schema
	) {
		String schemaValidado = validarSchema(schema);
		this.totalConsultasAnimalCall = callComAnimal(
				jdbcTemplate, schemaValidado, "PR_TOTAL_CONSULTAS_ANIMAL");
		this.totalConsultasFinalizadasCall = callComAnimal(
				jdbcTemplate, schemaValidado, "PR_TOTAL_CONSULTAS_FINALIZADAS");
		this.totalPacientesAtivosCall = callSemEntrada(
				jdbcTemplate, schemaValidado, "PR_TOTAL_PACIENTES_ATIVOS");
	}

	IndicadoresOracleRepository(
			SimpleJdbcCall totalConsultasAnimalCall,
			SimpleJdbcCall totalConsultasFinalizadasCall,
			SimpleJdbcCall totalPacientesAtivosCall
	) {
		this.totalConsultasAnimalCall = totalConsultasAnimalCall;
		this.totalConsultasFinalizadasCall = totalConsultasFinalizadasCall;
		this.totalPacientesAtivosCall = totalPacientesAtivosCall;
	}

	public long totalConsultasAnimal(long animalId) {
		return executarComAnimal(totalConsultasAnimalCall, "PR_TOTAL_CONSULTAS_ANIMAL", animalId);
	}

	public long totalConsultasFinalizadas(long animalId) {
		return executarComAnimal(
				totalConsultasFinalizadasCall, "PR_TOTAL_CONSULTAS_FINALIZADAS", animalId);
	}

	public long totalPacientesAtivos() {
		try {
			long total = extrairTotal(totalPacientesAtivosCall.execute());
			LOGGER.debug("Procedure {}.PR_TOTAL_PACIENTES_ATIVOS executada; total={}",
					PACKAGE_NAME, total);
			return total;
		} catch (DataAccessException | ArithmeticException ex) {
			throw new IndicadoresDatabaseException(ex);
		}
	}

	private long executarComAnimal(SimpleJdbcCall call, String procedure, long animalId) {
		try {
			MapSqlParameterSource parametros = new MapSqlParameterSource()
					.addValue(PARAM_ANIMAL_ID, animalId, Types.NUMERIC);
			long total = extrairTotal(call.execute(parametros));
			LOGGER.debug("Procedure {}.{} executada para animalId={}; total={}",
					PACKAGE_NAME, procedure, animalId, total);
			return total;
		} catch (DataAccessException | ArithmeticException ex) {
			throw new IndicadoresDatabaseException(ex);
		}
	}

	private long extrairTotal(Map<String, Object> resultado) {
		Object valor = resultado.get(PARAM_TOTAL);
		if (!(valor instanceof Number numero)) {
			throw new IndicadoresDatabaseException();
		}
		return new BigDecimal(numero.toString()).longValueExact();
	}

	private static SimpleJdbcCall callComAnimal(
			JdbcTemplate jdbcTemplate, String schema, String procedure
	) {
		return callBase(jdbcTemplate, schema, procedure)
				.declareParameters(
						new SqlParameter(PARAM_ANIMAL_ID, Types.NUMERIC),
						new SqlOutParameter(PARAM_TOTAL, Types.NUMERIC));
	}

	private static SimpleJdbcCall callSemEntrada(
			JdbcTemplate jdbcTemplate, String schema, String procedure
	) {
		return callBase(jdbcTemplate, schema, procedure)
				.declareParameters(new SqlOutParameter(PARAM_TOTAL, Types.NUMERIC));
	}

	private static SimpleJdbcCall callBase(
			JdbcTemplate jdbcTemplate, String schema, String procedure
	) {
		return new SimpleJdbcCall(jdbcTemplate)
				.withoutProcedureColumnMetaDataAccess()
				.withSchemaName(schema)
				.withCatalogName(PACKAGE_NAME)
				.withProcedureName(procedure);
	}

	private static String validarSchema(String schema) {
		String normalizado = schema == null ? "" : schema.trim().toUpperCase(Locale.ROOT);
		if (!ORACLE_IDENTIFIER.matcher(normalizado).matches()) {
			throw new IllegalArgumentException("Schema Oracle de indicadores invalido.");
		}
		return normalizado;
	}
}
