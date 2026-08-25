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

@Controller
public class SysAdminUsuarioController {

	private static final int DEFAULT_SIZE = 20;

	private final ObjectProvider<UsuarioService> usuarioService;

	public SysAdminUsuarioController(ObjectProvider<UsuarioService> usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping("/sysadmin/usuarios")
	public String listar(
			@RequestParam(defaultValue = "0") int page,
			Model model,
			Authentication authentication
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		int pagina = Math.max(page, 0);
		Pageable pageable = PageRequest.of(pagina, DEFAULT_SIZE, Sort.by("nome").ascending().and(Sort.by("login").ascending()));
		model.addAttribute("pageTitle", "Usuários");
		model.addAttribute("usuarios", usuarioService().listar(pageable));
		model.addAttribute("currentUserId", currentUserId(authentication));
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

}
