package br.com.fiap.arkive.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Custos assistenciais de consultas finalizadas pertencentes ao veterinario autenticado.")
public record VeterinarioCustoResponse(
		Long veterinarioId,
		String veterinarioNome,
		BigDecimal totalConsultas,
		BigDecimal totalMedicamentos,
		BigDecimal totalGeral,
		long quantidadeConsultasComValor,
		long quantidadePrescricoesComValor,
		List<ConsultaCusto> consultas
) {
	public VeterinarioCustoResponse {
		consultas = consultas == null ? List.of() : List.copyOf(consultas);
	}

	public record ConsultaCusto(
			Long consultaId,
			LocalDateTime dataHora,
			Long animalId,
			String animalNome,
			String animalArkivePatientId,
			@Schema(description = "Valor da consulta; nulo quando nenhum valor foi registrado.")
			BigDecimal valorConsulta,
			BigDecimal valorMedicamentos,
			BigDecimal valorTotal
	) {
	}
}
