package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class SysAdminUsuarioController {

	private static final int DEFAULT_SIZE = 20;
	private static final String DEFAULT_SORT_FIELD = "dataCadastro";
	private static final String DEFAULT_SORT_DIR = "desc";
	private static final Map<String, String> SORT_PROPERTIES = Map.of(
			"nome", "nome",
			"login", "login",
			"perfil", "tipo",
			"status", "ativo",
			"dataCadastro", "dataCadastro"
	);

	private final ObjectProvider<UsuarioService> usuarioService;

	public SysAdminUsuarioController(ObjectProvider<UsuarioService> usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping("/sysadmin/usuarios")
	public String listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "TODOS") String perfil,
			@RequestParam(defaultValue = DEFAULT_SORT_FIELD) String sortField,
			@RequestParam(defaultValue = DEFAULT_SORT_DIR) String sortDir,
			Model model,
			Authentication authentication
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		int pagina = Math.max(page, 0);
		PerfilFiltro filtro = PerfilFiltro.from(perfil);
		String campoOrdenacao = normalizedSortField(sortField);
		Sort.Direction direcao = normalizedSortDirection(sortDir);
		Pageable pageable = PageRequest.of(pagina, DEFAULT_SIZE, sort(campoOrdenacao, direcao));
		model.addAttribute("pageTitle", "Usuários");
		model.addAttribute("usuarios", usuarioService().listarPorTipos(filtro.tipos(), pageable));
		model.addAttribute("currentUserId", currentUserId(authentication));
		model.addAttribute("filtrosPerfil", PerfilFiltro.values());
		model.addAttribute("perfilSelecionado", filtro.getValue());
		model.addAttribute("sortField", campoOrdenacao);
		model.addAttribute("sortDir", direcao.name().toLowerCase(Locale.ROOT));
		return "sysadmin/usuarios/lista";
	}

	@GetMapping("/sysadmin/usuarios/novo")
	public String novo(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo Usuário");
		model.addAttribute("usuario", novoRequest());
		adicionarOpcoesFormulario(model);
		return "sysadmin/usuarios/formulario";
	}

	@PostMapping("/sysadmin/usuarios")
	public String criar(
			@Valid @ModelAttribute("usuario") UsuarioRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo Usuário");
		if (bindingResult.hasErrors()) {
			adicionarOpcoesFormulario(model);
			return "sysadmin/usuarios/formulario";
		}
		try {
			usuarioService().criar(request);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("usuario.invalido", ex.getMessage());
			adicionarOpcoesFormulario(model);
			return "sysadmin/usuarios/formulario";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Usuário criado com sucesso.");
		return "redirect:/sysadmin/usuarios";
	}

	@PostMapping("/sysadmin/usuarios/{id}/desativar")
	public String desativar(
			@PathVariable Long id,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		try {
			usuarioService().desativar(id, currentUserId(authentication));
			redirectAttributes.addFlashAttribute("sucesso", "Usuário desativado com sucesso.");
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/sysadmin/usuarios";
	}

	@PostMapping("/sysadmin/usuarios/{id}/ativar")
	public String ativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			usuarioService().ativar(id);
			redirectAttributes.addFlashAttribute("sucesso", "Usuário ativado com sucesso.");
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/sysadmin/usuarios";
	}

	private void adicionarOpcoesFormulario(Model model) {
		model.addAttribute("tipos", TipoUsuario.values());
		model.addAttribute("clinicas", usuarioService().listarClinicasAtivas());
		model.addAttribute("veterinarios", usuarioService().listarVeterinariosAtivos());
		model.addAttribute("responsaveis", usuarioService().listarResponsaveisAtivos());
	}

	private UsuarioRequest novoRequest() {
		return new UsuarioRequest("", null, "", "", null, null, null, "S");
	}

	private Long currentUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal) {
			return usuarioPrincipal.getUsuarioId();
		}
		return null;
	}

	private UsuarioService usuarioService() {
		return usuarioService.getObject();
	}

	private String normalizedSortField(String sortField) {
		return SORT_PROPERTIES.containsKey(sortField) ? sortField : DEFAULT_SORT_FIELD;
	}

	private Sort.Direction normalizedSortDirection(String sortDir) {
		return "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
	}

	private Sort sort(String sortField, Sort.Direction direction) {
		Sort sort = Sort.by(direction, SORT_PROPERTIES.get(sortField));
		if (!DEFAULT_SORT_FIELD.equals(sortField)) {
			sort = sort.and(Sort.by(Sort.Direction.DESC, "dataCadastro"));
		}
		return sort.and(Sort.by(Sort.Direction.DESC, "id"));
	}

	private enum PerfilFiltro {
		TODOS("TODOS", "Todos", List.of()),
		ADMINISTRATIVOS("ADMINISTRATIVOS", "Administrativos", List.of(TipoUsuario.SYSADMIN, TipoUsuario.ADMIN_CLINICA)),
		SYSADMIN("SYSADMIN", "SysAdmin", List.of(TipoUsuario.SYSADMIN)),
		ADMIN_CLINICA("ADMIN_CLINICA", "Administrador da Clínica", List.of(TipoUsuario.ADMIN_CLINICA)),
		VETERINARIO("VETERINARIO", "Veterinário", List.of(TipoUsuario.VETERINARIO)),
		RESPONSAVEL("RESPONSAVEL", "Responsável", List.of(TipoUsuario.RESPONSAVEL));

		private final String value;
		private final String label;
		private final List<TipoUsuario> tipos;

		PerfilFiltro(String value, String label, List<TipoUsuario> tipos) {
			this.value = value;
			this.label = label;
			this.tipos = tipos;
		}

		static PerfilFiltro from(String value) {
			if (value == null || value.isBlank()) {
				return TODOS;
			}
			String normalized = value.toUpperCase(Locale.ROOT);
			for (PerfilFiltro filtro : values()) {
				if (filtro.value.equals(normalized)) {
					return filtro;
				}
			}
			return TODOS;
		}

		public String getValue() {
			return value;
		}

		public String getLabel() {
			return label;
		}

		List<TipoUsuario> tipos() {
			return tipos;
		}
	}

}
