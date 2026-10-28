package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Prescricao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrescricaoResponse(
		Long id,
		String medicamento,
		String dosagem,
		String frequencia,
		String viaAdministracao,
		LocalDate dataInicio,
		LocalDate dataFim,
		String instrucoes,
		Long consultaId,
		@Schema(description = "Custo total do tratamento. Para veterinarios, nulo também pode indicar redacao por privacidade financeira.")
		BigDecimal valorMedicamento
) {
	public PrescricaoResponse(Long id, String medicamento, String dosagem, String frequencia,
			String viaAdministracao, LocalDate dataInicio, LocalDate dataFim, String instrucoes,
			Long consultaId) {
		this(id, medicamento, dosagem, frequencia, viaAdministracao, dataInicio, dataFim, instrucoes,
				consultaId, null);
	}

	public static PrescricaoResponse fromEntity(Prescricao prescricao) {
		return fromEntity(prescricao, true);
	}

	public static PrescricaoResponse fromEntity(Prescricao prescricao, boolean mostrarCustos) {
		return new PrescricaoResponse(
				prescricao.getId(),
				prescricao.getMedicamento(),
				prescricao.getDosagem(),
				prescricao.getFrequencia(),
				prescricao.getViaAdministracao(),
				prescricao.getDataInicio(),
				prescricao.getDataFim(),
				prescricao.getInstrucoes(),
				prescricao.getConsulta().getId(),
				mostrarCustos ? prescricao.getValorMedicamento() : null
		);
	}
}
