package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.EventoPreventivo;

import java.time.LocalDate;

public record EventoPreventivoResponse(
		Long id,
		LocalDate dataAplicacao,
		LocalDate dataProximo,
		String status,
		String alerta,
		String observacao,
		Long animalId,
		String animalNome,
		Long protocoloId,
		String protocoloNome,
		Long consultaId
) {
	public static EventoPreventivoResponse fromEntity(EventoPreventivo evento) {
		Long consultaId = evento.getConsulta() == null ? null : evento.getConsulta().getId();
		return new EventoPreventivoResponse(
				evento.getId(),
				evento.getDataAplicacao(),
				evento.getDataProximo(),
				evento.getStatus(),
				evento.getAlerta(),
				evento.getObservacao(),
				evento.getAnimal().getId(),
				evento.getAnimal().getNome(),
				evento.getProtocolo().getId(),
				evento.getProtocolo().getNome(),
				consultaId
		);
	}
}
