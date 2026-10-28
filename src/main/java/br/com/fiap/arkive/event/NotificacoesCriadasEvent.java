package br.com.fiap.arkive.event;

import java.util.List;

public record NotificacoesCriadasEvent(List<Long> notificacaoIds) {
	public NotificacoesCriadasEvent {
		notificacaoIds = List.copyOf(notificacaoIds);
	}
}
