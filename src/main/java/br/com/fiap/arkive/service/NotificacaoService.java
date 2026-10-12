package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.ContagemNotificacoesResponse;
import br.com.fiap.arkive.dto.response.NotificacaoResponse;
import br.com.fiap.arkive.dto.response.NotificacoesLidasResponse;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Notificacao;
import br.com.fiap.arkive.entity.TipoNotificacao;
import br.com.fiap.arkive.entity.TipoRecursoNotificacao;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.event.NotificacoesCriadasEvent;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.NotificacaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile("!local-nodb")
public class NotificacaoService {

	private static final String TITULO_ATUALIZACAO = "Atualização no ArkIve";
	private static final String CHAVE_CONSULTA_FINALIZADA = "CONSULTA_FINALIZADA:";

	private final NotificacaoRepository notificacaoRepository;
	private final NotificacaoRecipientService recipientService;
	private final ApplicationEventPublisher eventPublisher;

	public NotificacaoService(NotificacaoRepository notificacaoRepository,
			NotificacaoRecipientService recipientService, ApplicationEventPublisher eventPublisher) {
		this.notificacaoRepository = notificacaoRepository;
		this.recipientService = recipientService;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public List<Notificacao> criarParaConsultaFinalizada(Consulta consulta) {
		String chaveEvento = CHAVE_CONSULTA_FINALIZADA + consulta.getId();
		List<Notificacao> criadas = new ArrayList<>();
		for (Usuario destinatario : recipientService.responsaveisAtivosVigentes(consulta.getAnimal())) {
			if (notificacaoRepository.findByUsuarioIdAndChaveEvento(destinatario.getId(), chaveEvento).isPresent()) {
				continue;
			}
			Notificacao notificacao = new Notificacao();
			notificacao.setUsuario(destinatario);
			notificacao.setTipo(TipoNotificacao.CONSULTA_FINALIZADA);
			notificacao.setTitulo(TITULO_ATUALIZACAO);
			notificacao.setMensagem("Há uma nova atualização no acompanhamento de "
					+ consulta.getAnimal().getNome() + ".");
			notificacao.setAnimal(consulta.getAnimal());
			notificacao.setTipoRecurso(TipoRecursoNotificacao.CONSULTA);
			notificacao.setRecursoId(consulta.getId());
			notificacao.setChaveEvento(chaveEvento);
			criadas.add(notificacaoRepository.save(notificacao));
		}
		List<Long> ids = criadas.stream().map(Notificacao::getId).toList();
		if (!ids.isEmpty()) {
			eventPublisher.publishEvent(new NotificacoesCriadasEvent(ids));
		}
		return List.copyOf(criadas);
	}

	@Transactional(readOnly = true)
	public Page<NotificacaoResponse> listar(boolean somenteNaoLidas, Pageable pageable, UsuarioPrincipal principal) {
		Long usuarioId = exigirUsuario(principal);
		Page<Notificacao> pagina = somenteNaoLidas
				? notificacaoRepository.findByUsuarioIdAndDataLeituraIsNullOrderByDataCriacaoDescIdDesc(usuarioId, pageable)
				: notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDescIdDesc(usuarioId, pageable);
		return pagina.map(NotificacaoResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public ContagemNotificacoesResponse contarNaoLidas(UsuarioPrincipal principal) {
		return new ContagemNotificacoesResponse(
				notificacaoRepository.countByUsuarioIdAndDataLeituraIsNull(exigirUsuario(principal))
		);
	}

	@Transactional
	public NotificacaoResponse marcarComoLida(Long id, UsuarioPrincipal principal) {
		Long usuarioId = exigirUsuario(principal);
		Notificacao notificacao = notificacaoRepository.findByIdAndUsuarioId(id, usuarioId)
				.orElseThrow(() -> new ResourceNotFoundException("Notificacao nao encontrada."));
		if (notificacao.getDataLeitura() == null) {
			notificacao.setDataLeitura(LocalDateTime.now());
			notificacaoRepository.save(notificacao);
		}
		return NotificacaoResponse.fromEntity(notificacao);
	}

	@Transactional
	public NotificacoesLidasResponse marcarTodasComoLidas(UsuarioPrincipal principal) {
		int atualizadas = notificacaoRepository.marcarTodasComoLidas(exigirUsuario(principal), LocalDateTime.now());
		return new NotificacoesLidasResponse(atualizadas);
	}

	private Long exigirUsuario(UsuarioPrincipal principal) {
		if (principal == null || principal.getUsuarioId() == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
		return principal.getUsuarioId();
	}
}

