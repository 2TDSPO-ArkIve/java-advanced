package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.repository.PrescricaoRepository.CustoMedicamentoPorConsulta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class CustoAssistencialCalculator {

	static final BigDecimal ZERO_MONETARIO = new BigDecimal("0.00");

	private CustoAssistencialCalculator() {
	}

	static Resultado calcular(
			List<Consulta> consultas,
			List<CustoMedicamentoPorConsulta> agregadosMedicamentos
	) {
		Map<Long, CustoMedicamentoPorConsulta> medicamentosPorConsulta = agregadosMedicamentos.stream()
				.collect(Collectors.toMap(CustoMedicamentoPorConsulta::getConsultaId, Function.identity()));
		BigDecimal totalConsultas = ZERO_MONETARIO;
		BigDecimal totalMedicamentos = ZERO_MONETARIO;
		long quantidadeConsultasComValor = 0;
		long quantidadePrescricoesComValor = 0;
		List<Linha> linhas = new ArrayList<>(consultas.size());

		for (Consulta consulta : consultas) {
			BigDecimal valorConsulta = consulta.getValorConsulta() == null
					? null : monetario(consulta.getValorConsulta());
			CustoMedicamentoPorConsulta agregado = medicamentosPorConsulta.get(consulta.getId());
			BigDecimal valorMedicamentos = agregado == null
					? ZERO_MONETARIO : monetario(agregado.getValorMedicamentos());
			long prescricoesComValor = agregado == null || agregado.getQuantidadePrescricoesComValor() == null
					? 0 : agregado.getQuantidadePrescricoesComValor();

			if (valorConsulta != null) {
				totalConsultas = totalConsultas.add(valorConsulta);
				quantidadeConsultasComValor++;
			}
			totalMedicamentos = totalMedicamentos.add(valorMedicamentos);
			quantidadePrescricoesComValor += prescricoesComValor;
			linhas.add(new Linha(
					consulta,
					valorConsulta,
					valorMedicamentos,
					(valorConsulta == null ? ZERO_MONETARIO : valorConsulta).add(valorMedicamentos)
			));
		}

		return new Resultado(
				totalConsultas,
				totalMedicamentos,
				totalConsultas.add(totalMedicamentos),
				quantidadeConsultasComValor,
				quantidadePrescricoesComValor,
				linhas
		);
	}

	private static BigDecimal monetario(BigDecimal valor) {
		return valor == null ? ZERO_MONETARIO : valor.setScale(2, RoundingMode.UNNECESSARY);
	}

	record Linha(
			Consulta consulta,
			BigDecimal valorConsulta,
			BigDecimal valorMedicamentos,
			BigDecimal valorTotal
	) {
	}

	record Resultado(
			BigDecimal totalConsultas,
			BigDecimal totalMedicamentos,
			BigDecimal totalGeral,
			long quantidadeConsultasComValor,
			long quantidadePrescricoesComValor,
			List<Linha> linhas
	) {
		Resultado {
			linhas = List.copyOf(linhas);
		}
	}
}
