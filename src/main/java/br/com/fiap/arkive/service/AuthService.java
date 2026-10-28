package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistroVeterinarioRequest;
import br.com.fiap.arkive.dto.request.RegistroResponsavelRequest;
import br.com.fiap.arkive.dto.request.VeterinarioRequest;
import br.com.fiap.arkive.dto.response.AuthMeResponse;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.dto.response.RegistroResponsavelResponse;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.PasswordPolicy;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordLifecycleService passwordLifecycleService;
	private final VeterinarioService veterinarioService;
	private final ResponsavelRegistrationService responsavelRegistrationService;
	private final PasswordPolicy passwordPolicy;

	public AuthService(
			UsuarioRepository usuarioRepository,
			PasswordLifecycleService passwordLifecycleService,
			VeterinarioService veterinarioService,
			ResponsavelRegistrationService responsavelRegistrationService,
			PasswordPolicy passwordPolicy
	) {
		this.usuarioRepository = usuarioRepository;
		this.passwordLifecycleService = passwordLifecycleService;
		this.veterinarioService = veterinarioService;
		this.responsavelRegistrationService = responsavelRegistrationService;
		this.passwordPolicy = passwordPolicy;
	}

	@Transactional(readOnly = true)
	public AuthMeResponse me(UsuarioPrincipal principal) {
		return AuthMeResponse.fromEntity(buscarUsuario(principal));
	}

	@Transactional
	public void alterarSenha(UsuarioPrincipal principal, String novaSenha) {
		passwordLifecycleService.alterarSenhaObrigatoria(usuarioId(principal), novaSenha, novaSenha);
	}

	/**
	 * Public veterinarian self-registration entry point (`POST /api/auth/register`;
	 * see SecurityConfig). The caller can never choose a role or a clinic:
	 * this delegates to a dedicated transactional self-registration path.
	 * The role remains server-assigned as `TipoUsuario.VETERINARIO`, while the
	 * caller's chosen password is validated, BCrypt-hashed and stored with
	 * `trocaSenha=N`. Administrator-created veterinarian accounts continue to
	 * use `VeterinarioService.criar`, temporary credentials and mandatory
	 * password change.
	 */
	@Transactional
	public VeterinarioResponse registrarVeterinario(RegistroVeterinarioRequest request) {
		passwordPolicy.validar(request.senha());
		passwordPolicy.validarConfirmacao(request.senha(), request.confirmacaoSenha());
		return veterinarioService.criarAutocadastro(new VeterinarioRequest(
				request.nome(),
				request.crmv(),
				null,
				request.email(),
				null,
				null
		), request.senha());
	}

	@Transactional
	public RegistroResponsavelResponse registrarResponsavel(RegistroResponsavelRequest request) {
		return responsavelRegistrationService.registrar(request);
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
