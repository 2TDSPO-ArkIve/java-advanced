package br.com.fiap.arkive.service.notification;

import br.com.fiap.arkive.event.NotificacoesCriadasEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("!local-nodb")
public class NotificacaoPushListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificacaoPushListener.class);
	private final NotificacaoPushService pushService;

	public NotificacaoPushListener(NotificacaoPushService pushService) {
		this.pushService = pushService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void aposCommit(NotificacoesCriadasEvent event) {
		try {
			pushService.entregar(event.notificacaoIds());
		} catch (RuntimeException ex) {
			LOGGER.warn("Falha sanitizada no processamento de push apos commit para {} notificacoes.",
					event.notificacaoIds().size());
		}
	}
}
