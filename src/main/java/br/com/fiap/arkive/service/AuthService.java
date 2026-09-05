package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.AuthMeResponse;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordLifecycleService passwordLifecycleService;

	public AuthService(
			UsuarioRepository usuarioRepository,
			PasswordLifecycleService passwordLifecycleService
	) {
		this.usuarioRepository = usuarioRepository;
		this.passwordLifecycleService = passwordLifecycleService;
	}

	@Transactional(readOnly = true)
	public AuthMeResponse me(UsuarioPrincipal principal) {
		return AuthMeResponse.fromEntity(buscarUsuario(principal));
	}

	@Transactional
	public void alterarSenha(UsuarioPrincipal principal, String novaSenha) {
		passwordLifecycleService.alterarSenhaObrigatoria(usuarioId(principal), novaSenha, novaSenha);
	}

	private Usuario buscarUsuario(UsuarioPrincipal principal) {
		return usuarioRepository.findById(usuarioId(principal))
				.orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
	}

	private Long usuarioId(UsuarioPrincipal principal) {
		if (principal == null || principal.getUsuarioId() == null) {
			throw new BusinessException("Usuario autenticado invalido.");
		}
		return principal.getUsuarioId();
	}
}
