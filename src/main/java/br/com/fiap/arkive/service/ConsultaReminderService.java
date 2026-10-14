package br.com.fiap.arkive.service;

import br.com.fiap.arkive.config.ConsultaReminderProperties;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.repository.ConsultaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("!local-nodb")
public class ConsultaReminderService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsultaReminderService.class);

	private final ConsultaRepository consultaRepository;
	private final NotificacaoRecipientService recipientService;
	private final NotificacaoService notificacaoService;
	private final ConsultaReminderProperties properties;
	private final Clock clock;

	public ConsultaReminderService(
			ConsultaRepository consultaRepository,
			NotificacaoRecipientService recipientService,
			NotificacaoService notificacaoService,
			ConsultaReminderProperties properties,
			@Qualifier("businessClock") Clock clock
	) {
		this.consultaRepository = consultaRepository;
		this.recipientService = recipientService;
		this.notificacaoService = notificacaoService;
		this.properties = properties;
		this.clock = clock;
	}

	public int processarLembretes() {
		LocalDateTime agora = LocalDateTime.now(clock);
		LocalDateTime limite = agora.plusMinutes(properties.getMinutesBefore());
		List<Consulta> consultas = consultaRepository.buscarAgendadasParaLembrete(agora, limite);
		int criadas = 0;
		for (Consulta consulta : consultas) {
			if (!consultaValidaParaNotificacao(consulta)) {
				LOGGER.warn("Consulta agendada ignorada no lembrete por dados obrigatorios ausentes.");
				continue;
			}
			var destinatario = recipientService.veterinarioAtivo(consulta.getVeterinario());
			if (destinatario.isEmpty()) {
				LOGGER.warn("Consulta {} sem conta veterinaria ativa vinculada para lembrete.", consulta.getId());
				continue;
			}
			if (notificacaoService.criarLembreteConsulta(consulta, destinatario.get()).isPresent()) {
				criadas++;
			}
		}
		return criadas;
	}

	private boolean consultaValidaParaNotificacao(Consulta consulta) {
		return consulta != null
				&& consulta.getId() != null
				&& consulta.getDataHora() != null
				&& consulta.getVeterinario() != null
				&& consulta.getAnimal() != null
				&& consulta.getAnimal().getNome() != null
				&& consulta.getAnimal().getArkivePatientId() != null;
	}
}
