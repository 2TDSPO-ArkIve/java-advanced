package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Prescricao;

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
		Long consultaId
) {
	public static PrescricaoResponse fromEntity(Prescricao prescricao) {
		return new PrescricaoResponse(
				prescricao.getId(),
				prescricao.getMedicamento(),
				prescricao.getDosagem(),
				prescricao.getFrequencia(),
				prescricao.getViaAdministracao(),
				prescricao.getDataInicio(),
				prescricao.getDataFim(),
				prescricao.getInstrucoes(),
				prescricao.getConsulta().getId()
		);
	}
}
