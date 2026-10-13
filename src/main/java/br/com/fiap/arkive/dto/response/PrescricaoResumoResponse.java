package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Prescricao;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PrescricaoResumoResponse(
		String medicamento, String dosagem, String frequencia, String viaAdministracao,
		LocalDate dataInicio, LocalDate dataFim, String instrucoes, BigDecimal valorMedicamento
) {
	public PrescricaoResumoResponse(String medicamento, String dosagem, String frequencia,
			String viaAdministracao, LocalDate dataInicio, LocalDate dataFim, String instrucoes) {
		this(medicamento, dosagem, frequencia, viaAdministracao, dataInicio, dataFim, instrucoes, null);
	}

	public static PrescricaoResumoResponse fromEntity(Prescricao prescricao) {
		return new PrescricaoResumoResponse(prescricao.getMedicamento(), prescricao.getDosagem(),
				prescricao.getFrequencia(), prescricao.getViaAdministracao(), prescricao.getDataInicio(),
				prescricao.getDataFim(), prescricao.getInstrucoes(), prescricao.getValorMedicamento());
	}
}
