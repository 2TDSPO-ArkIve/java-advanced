package br.com.fiap.arkive.service.notification;

import br.com.fiap.arkive.service.ConsultaReminderService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!local-nodb")
public class ConsultaReminderScheduler {

	private final ConsultaReminderService reminderService;

	public ConsultaReminderScheduler(ConsultaReminderService reminderService) {
		this.reminderService = reminderService;
	}

	@Scheduled(fixedDelayString = "${arkive.notifications.consultation-reminder.poll-ms:60000}")
	public void processar() {
		reminderService.processarLembretes();
	}
}
