package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Notificacao;
import br.com.fiap.arkive.entity.TipoNotificacao;
import br.com.fiap.arkive.entity.TipoRecursoNotificacao;

import java.time.LocalDateTime;

public record NotificacaoResponse(
		Long id,
		TipoNotificacao type,
		String title,
		String message,
		LocalDateTime createdAt,
		LocalDateTime readAt,
		Long animalId,
		String arkivePatientId,
		TipoRecursoNotificacao resourceType,
		Long resourceId
) {
	public static NotificacaoResponse fromEntity(Notificacao notificacao) {
		Long animalId = notificacao.getAnimal() == null ? null : notificacao.getAnimal().getId();
		String arkivePatientId = notificacao.getAnimal() == null ? null : notificacao.getAnimal().getArkivePatientId();
		return new NotificacaoResponse(
				notificacao.getId(),
				notificacao.getTipo(),
				notificacao.getTitulo(),
				notificacao.getMensagem(),
				notificacao.getDataCriacao(),
				notificacao.getDataLeitura(),
				animalId,
				arkivePatientId,
				notificacao.getTipoRecurso(),
				notificacao.getRecursoId()
		);
	}
}

