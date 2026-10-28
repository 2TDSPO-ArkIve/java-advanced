package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.ContagemNotificacoesResponse;
import br.com.fiap.arkive.dto.response.NotificacaoResponse;
import br.com.fiap.arkive.dto.response.NotificacoesLidasResponse;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Notificacao;
import br.com.fiap.arkive.entity.PatientAccessRequest;
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
import java.util.Optional;

@Service
@Profile("!local-nodb")
public class NotificacaoService {

	private static final String TITULO_ATUALIZACAO = "Atualização no ArkIve";
	private static final String TITULO_CONSULTA_AGENDADA = "Consulta agendada";
	private static final String TITULO_LEMBRETE_CONSULTA = "Lembrete de consulta";
	private static final String CHAVE_CONSULTA_AGENDADA = "CONSULTA_AGENDADA:";
	private static final String CHAVE_LEMBRETE_CONSULTA = "LEMBRETE_CONSULTA:";
	private static final String CHAVE_CONSULTA_FINALIZADA = "CONSULTA_FINALIZADA:";
	private static final String TITULO_SOLICITACAO_ACESSO = "Solicitação de acesso";
	private static final String CHAVE_SOLICITACAO_ACESSO = "SOLICITACAO_ACESSO:";

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
		return criarParaResponsaveis(
				consulta,
				TipoNotificacao.CONSULTA_FINALIZADA,
				TITULO_ATUALIZACAO,
				"Há uma nova atualização no acompanhamento de " + consulta.getAnimal().getNome() + ".",
				CHAVE_CONSULTA_FINALIZADA + consulta.getId()
		);
	}

	@Transactional
	public List<Notificacao> criarParaConsultaAgendada(Consulta consulta) {
		return criarParaResponsaveis(
				consulta,
				TipoNotificacao.CONSULTA_AGENDADA,
				TITULO_CONSULTA_AGENDADA,
				"Uma consulta foi agendada para " + identificacaoAnimal(consulta) + ".",
				CHAVE_CONSULTA_AGENDADA + consulta.getId()
		);
	}

	@Transactional
	public Optional<Notificacao> criarLembreteConsulta(Consulta consulta, Usuario destinatario) {
		String chaveEvento = CHAVE_LEMBRETE_CONSULTA + consulta.getId() + ":" + consulta.getDataHora();
		Optional<Notificacao> criada = criarSeAusente(
				destinatario,
				consulta,
				TipoNotificacao.LEMBRETE_CONSULTA,
				TITULO_LEMBRETE_CONSULTA,
				"Consulta de " + identificacaoAnimal(consulta) + " em breve.",
				chaveEvento
		);
		criada.ifPresent(notificacao -> publicarCriadas(List.of(notificacao)));
		return criada;
	}

	@Transactional
	public List<Notificacao> criarParaSolicitacaoAcesso(
			PatientAccessRequest solicitacao,
			List<Usuario> destinatarios
	) {
		var veterinario = solicitacao.getRequester().getVeterinario();
		var animal = solicitacao.getAnimal();
		String mensagem = veterinario.getNome() + " (CRMV " + veterinario.getCrmv()
				+ ") solicitou acesso a " + animal.getNome() + " (" + animal.getArkivePatientId() + ").";
		String chaveEvento = CHAVE_SOLICITACAO_ACESSO + solicitacao.getId();
		List<Notificacao> criadas = new ArrayList<>();
		for (Usuario destinatario : destinatarios) {
			criarSeAusente(
					destinatario,
					animal,
					TipoNotificacao.SOLICITACAO_ACESSO,
					TITULO_SOLICITACAO_ACESSO,
					mensagem,
					TipoRecursoNotificacao.SOLICITACAO_ACESSO,
					solicitacao.getId(),
					chaveEvento
			).ifPresent(criadas::add);
		}
		publicarCriadas(criadas);
		return List.copyOf(criadas);
	}

	private List<Notificacao> criarParaResponsaveis(
			Consulta consulta,
			TipoNotificacao tipo,
			String titulo,
			String mensagem,
			String chaveEvento
	) {
		List<Notificacao> criadas = new ArrayList<>();
		for (Usuario destinatario : recipientService.responsaveisAtivosVigentes(consulta.getAnimal())) {
			criarSeAusente(destinatario, consulta, tipo, titulo, mensagem, chaveEvento)
					.ifPresent(criadas::add);
		}
		publicarCriadas(criadas);
		return List.copyOf(criadas);
	}

	private Optional<Notificacao> criarSeAusente(
			Usuario destinatario,
			Consulta consulta,
			TipoNotificacao tipo,
			String titulo,
			String mensagem,
			String chaveEvento
	) {
		return criarSeAusente(destinatario, consulta.getAnimal(), tipo, titulo, mensagem,
				TipoRecursoNotificacao.CONSULTA, consulta.getId(), chaveEvento);
	}

	private Optional<Notificacao> criarSeAusente(
			Usuario destinatario,
			br.com.fiap.arkive.entity.Animal animal,
			TipoNotificacao tipo,
			String titulo,
			String mensagem,
			TipoRecursoNotificacao tipoRecurso,
			Long recursoId,
			String chaveEvento
	) {
		if (notificacaoRepository.findByUsuarioIdAndChaveEvento(destinatario.getId(), chaveEvento).isPresent()) {
			return Optional.empty();
		}
		Notificacao notificacao = new Notificacao();
		notificacao.setUsuario(destinatario);
		notificacao.setTipo(tipo);
		notificacao.setTitulo(titulo);
		notificacao.setMensagem(mensagem);
		notificacao.setAnimal(animal);
		notificacao.setTipoRecurso(tipoRecurso);
		notificacao.setRecursoId(recursoId);
		notificacao.setChaveEvento(chaveEvento);
		return Optional.of(notificacaoRepository.save(notificacao));
	}

	private void publicarCriadas(List<Notificacao> criadas) {
		List<Long> ids = criadas.stream().map(Notificacao::getId).toList();
		if (!ids.isEmpty()) {
			eventPublisher.publishEvent(new NotificacoesCriadasEvent(ids));
		}
	}

	private String identificacaoAnimal(Consulta consulta) {
		return consulta.getAnimal().getNome() + " (" + consulta.getAnimal().getArkivePatientId() + ")";
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
