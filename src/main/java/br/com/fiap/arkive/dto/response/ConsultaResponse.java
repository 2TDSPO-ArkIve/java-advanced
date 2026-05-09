package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Consulta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultaResponse(
		Long id,
		LocalDateTime dataHora,
		String modalidade,
		String motivo,
		String sintomasRelatados,
		String observacoes,
		BigDecimal pesoNaConsulta,
		String transcricaoRaw,
		String status,
		Long animalId,
		String animalNome,
		Long veterinarioId,
		String veterinarioNome,
		Long clinicaId,
		String clinicaNome
) {
	public static ConsultaResponse fromEntity(Consulta consulta) {
		Long clinicaId = consulta.getClinica() == null ? null : consulta.getClinica().getId();
		String clinicaNome = consulta.getClinica() == null ? null : consulta.getClinica().getNome();
		return new ConsultaResponse(
				consulta.getId(),
				consulta.getDataHora(),
				consulta.getModalidade(),
				consulta.getMotivo(),
				consulta.getSintomasRelatados(),
				consulta.getObservacoes(),
				consulta.getPesoNaConsulta(),
				consulta.getTranscricaoRaw(),
				consulta.getStatus(),
				consulta.getAnimal().getId(),
				consulta.getAnimal().getNome(),
				consulta.getVeterinario().getId(),
				consulta.getVeterinario().getNome(),
				clinicaId,
				clinicaNome
		);
	}
}
