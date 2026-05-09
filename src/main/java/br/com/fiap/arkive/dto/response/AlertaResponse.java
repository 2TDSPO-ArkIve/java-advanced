package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Alerta;

import java.time.LocalDateTime;

public record AlertaResponse(
		Long id,
		String tipo,
		String mensagem,
		LocalDateTime dataEnvio,
		LocalDateTime dataLeitura,
		String status,
		String canal,
		Long animalId,
		String animalNome,
		Long responsavelId,
		String responsavelNome,
		Long clinicaId,
		String clinicaNome,
		Long eventoPreventivoId
) {
	public static AlertaResponse fromEntity(Alerta alerta) {
		Long responsavelId = alerta.getResponsavel() == null ? null : alerta.getResponsavel().getId();
		String responsavelNome = alerta.getResponsavel() == null ? null : alerta.getResponsavel().getNome();
		Long clinicaId = alerta.getClinica() == null ? null : alerta.getClinica().getId();
		String clinicaNome = alerta.getClinica() == null ? null : alerta.getClinica().getNome();
		Long eventoId = alerta.getEventoPreventivo() == null ? null : alerta.getEventoPreventivo().getId();
		return new AlertaResponse(
				alerta.getId(),
				alerta.getTipo(),
				alerta.getMensagem(),
				alerta.getDataEnvio(),
				alerta.getDataLeitura(),
				alerta.getStatus(),
				alerta.getCanal(),
				alerta.getAnimal().getId(),
				alerta.getAnimal().getNome(),
				responsavelId,
				responsavelNome,
				clinicaId,
				clinicaNome,
				eventoId
		);
	}
}
