package br.com.fiap.arkive.service.notification;

import br.com.fiap.arkive.service.ConsultaReminderService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConsultaReminderSchedulerTest {

	@Test
	void tarefaAgendadaDelegaParaServicoTestavel() {
		ConsultaReminderService service = mock(ConsultaReminderService.class);

		new ConsultaReminderScheduler(service).processar();

		verify(service).processarLembretes();
	}
}
