package br.com.fiap.arkive.service.notification;

import br.com.fiap.arkive.event.NotificacoesCriadasEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificacaoPushListenerTest {

	@Test
	void listenerEhAfterCommitEDelegaIdsCriados() throws Exception {
		Method method = NotificacaoPushListener.class.getMethod("aposCommit", NotificacoesCriadasEvent.class);
		TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);
		assertNotNull(annotation);
		assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
		NotificacaoPushService pushService = mock(NotificacaoPushService.class);
		NotificacoesCriadasEvent event = new NotificacoesCriadasEvent(List.of(1L, 2L));

		new NotificacaoPushListener(pushService).aposCommit(event);

		verify(pushService).entregar(List.of(1L, 2L));
	}

	@Test
	void falhaDepoisDoCommitEhContida() {
		NotificacaoPushService pushService = mock(NotificacaoPushService.class);
		doThrow(new IllegalStateException("falha simulada")).when(pushService).entregar(List.of(1L));

		assertDoesNotThrow(() -> new NotificacaoPushListener(pushService)
				.aposCommit(new NotificacoesCriadasEvent(List.of(1L))));
	}
}
