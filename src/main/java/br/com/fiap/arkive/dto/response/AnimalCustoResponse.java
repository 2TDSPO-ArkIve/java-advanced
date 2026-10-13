package br.com.fiap.arkive.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Resumo de custos assistenciais finalizados do paciente. Valores nulos nao registrados contam como zero apenas nos totais.")
public record AnimalCustoResponse(
		Long animalId,
		String arkivePatientId,
		String animalNome,
		BigDecimal totalConsultas,
		BigDecimal totalMedicamentos,
		BigDecimal totalGeral,
		long quantidadeConsultasComValor,
		long quantidadePrescricoesComValor,
		List<ConsultaCusto> consultas
) {
	public AnimalCustoResponse {
		consultas = consultas == null ? List.of() : List.copyOf(consultas);
	}

	public record ConsultaCusto(
			Long consultaId,
			LocalDateTime dataHora,
			String status,
			String statusDescricao,
			@Schema(description = "Valor registrado da consulta; nulo quando nao informado.")
			BigDecimal valorConsulta,
			BigDecimal valorMedicamentos,
			BigDecimal valorTotal
	) {
	}
}
