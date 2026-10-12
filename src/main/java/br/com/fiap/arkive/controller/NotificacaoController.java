package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.response.ContagemNotificacoesResponse;
import br.com.fiap.arkive.dto.response.NotificacaoResponse;
import br.com.fiap.arkive.dto.response.NotificacoesLidasResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.NotificacaoService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
@Profile("!local-nodb")
public class NotificacaoController {

	private final NotificacaoService notificacaoService;

	public NotificacaoController(NotificacaoService notificacaoService) {
		this.notificacaoService = notificacaoService;
	}

	@GetMapping
	public Page<NotificacaoResponse> listar(
			@RequestParam(name = "naoLidas", defaultValue = "false") boolean somenteNaoLidas,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return notificacaoService.listar(somenteNaoLidas, pageable, principal);
	}

	@GetMapping("/nao-lidas/contagem")
	public ContagemNotificacoesResponse contarNaoLidas(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return notificacaoService.contarNaoLidas(principal);
	}

	@PatchMapping("/{id}/lida")
	public NotificacaoResponse marcarComoLida(@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return notificacaoService.marcarComoLida(id, principal);
	}

	@PatchMapping("/lidas")
	public NotificacoesLidasResponse marcarTodasComoLidas(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return notificacaoService.marcarTodasComoLidas(principal);
	}
}

