package br.com.fiap.arkive.service.notification;

import br.com.fiap.arkive.config.ExpoPushProperties;
import br.com.fiap.arkive.entity.Notificacao;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.UsuarioDispositivo;
import br.com.fiap.arkive.repository.NotificacaoRepository;
import br.com.fiap.arkive.repository.UsuarioDispositivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("!local-nodb")
public class NotificacaoPushService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificacaoPushService.class);

	private final NotificacaoRepository notificacaoRepository;
	private final UsuarioDispositivoRepository dispositivoRepository;
	private final ExpoPushClient expoPushClient;
	private final ExpoPushProperties properties;

	public NotificacaoPushService(NotificacaoRepository notificacaoRepository,
			UsuarioDispositivoRepository dispositivoRepository, ExpoPushClient expoPushClient,
			ExpoPushProperties properties) {
		this.notificacaoRepository = notificacaoRepository;
		this.dispositivoRepository = dispositivoRepository;
		this.expoPushClient = expoPushClient;
		this.properties = properties;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void entregar(List<Long> notificacaoIds) {
		if (!properties.isEnabled()) {
			return;
		}
		for (Long notificacaoId : notificacaoIds) {
			notificacaoRepository.findById(notificacaoId).ifPresent(this::entregar);
		}
	}

	private void entregar(Notificacao notificacao) {
		Usuario usuario = notificacao.getUsuario();
		if (TipoUsuario.RESPONSAVEL.equals(usuario.getTipo())
				&& usuario.getResponsavel() != null
				&& "N".equals(usuario.getResponsavel().getNotificacao())) {
			return;
		}
		List<UsuarioDispositivo> dispositivos = dispositivoRepository
				.findByUsuarioIdAndAtivoOrderByIdAsc(usuario.getId(), "S");
		for (UsuarioDispositivo dispositivo : dispositivos) {
			try {
				ExpoPushResult resultado = expoPushClient.enviar(new ExpoPushMessage(
						dispositivo.getExpoPushToken(),
						notificacao.getTitulo(),
						notificacao.getMensagem(),
						criarPayload(notificacao)
				));
				if (ExpoPushResult.DEVICE_NOT_REGISTERED.equals(resultado)) {
					desativarDispositivo(dispositivo);
				}
			} catch (RuntimeException ex) {
				LOGGER.warn("Falha inesperada e sanitizada ao entregar notificacao {}.", notificacao.getId());
			}
		}
	}

	private Map<String, Object> criarPayload(Notificacao notificacao) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("notificationId", notificacao.getId());
		payload.put("type", notificacao.getTipo().name());
		if (notificacao.getAnimal() != null) payload.put("animalId", notificacao.getAnimal().getId());
		if (notificacao.getTipoRecurso() != null) payload.put("resourceType", notificacao.getTipoRecurso().name());
		if (notificacao.getRecursoId() != null) payload.put("resourceId", notificacao.getRecursoId());
		return payload;
	}

	private void desativarDispositivo(UsuarioDispositivo dispositivo) {
		dispositivo.setAtivo("N");
		dispositivo.setDataRevogacao(LocalDateTime.now());
		dispositivo.setDataUltimoContato(LocalDateTime.now());
		dispositivoRepository.save(dispositivo);
	}
}

