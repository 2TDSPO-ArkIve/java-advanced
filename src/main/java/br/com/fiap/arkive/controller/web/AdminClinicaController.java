package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.dto.response.AdesaoPrescricaoResponse;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.dto.response.EspecieResponse;
import br.com.fiap.arkive.dto.response.PrescricaoResponse;
import br.com.fiap.arkive.dto.response.RacaResponse;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AdesaoPrescricaoService;
import br.com.fiap.arkive.service.AnimalService;
import br.com.fiap.arkive.service.ConsultaService;
import br.com.fiap.arkive.service.EspecieService;
import br.com.fiap.arkive.service.PrescricaoService;
import br.com.fiap.arkive.service.RacaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@Controller
public class AdminClinicaController {

	private static final int DEFAULT_SIZE = 20;
	private static final int OPTION_SIZE = 200;

	private final ObjectProvider<AnimalService> animalService;
	private final ObjectProvider<ConsultaService> consultaService;
	private final ObjectProvider<PrescricaoService> prescricaoService;
	private final ObjectProvider<AdesaoPrescricaoService> adesaoPrescricaoService;
	private final ObjectProvider<EspecieService> especieService;
	private final ObjectProvider<RacaService> racaService;

	public AdminClinicaController(
			ObjectProvider<AnimalService> animalService,
			ObjectProvider<ConsultaService> consultaService,
			ObjectProvider<PrescricaoService> prescricaoService,
			ObjectProvider<AdesaoPrescricaoService> adesaoPrescricaoService,
			ObjectProvider<EspecieService> especieService,
			ObjectProvider<RacaService> racaService
	) {
		this.animalService = animalService;
		this.consultaService = consultaService;
		this.prescricaoService = prescricaoService;
		this.adesaoPrescricaoService = adesaoPrescricaoService;
		this.especieService = especieService;
		this.racaService = racaService;
	}

	@GetMapping("/admin/animais")
	public String listarAnimais(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) String ativo,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("nome").ascending().and(Sort.by("id").ascending()));
		model.addAttribute("pageTitle", "Animais");
		model.addAttribute("animais", listarAnimais(nome, ativo, pageable, principal));
		model.addAttribute("nome", nome);
		model.addAttribute("ativoSelecionado", ativo);
		return "admin/animais/lista";
	}

	@GetMapping("/admin/animais/novo")
	public String novoAnimal(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo animal");
		model.addAttribute("animal", novoAnimalRequest());
		adicionarOpcoesAnimal(model);
		return "admin/animais/formulario";
	}

	@PostMapping("/admin/animais")
	public String criarAnimal(
			@Valid @ModelAttribute("animal") AnimalRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo animal");
		if (bindingResult.hasErrors()) {
			adicionarOpcoesAnimal(model);
			return "admin/animais/formulario";
		}
		try {
			animalService().criar(request, principal);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("animal.invalido", ex.getMessage());
			adicionarOpcoesAnimal(model);
			return "admin/animais/formulario";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Animal cadastrado com sucesso.");
		return "redirect:/admin/animais";
	}

	@GetMapping("/admin/animais/{id}/editar")
	public String editarAnimal(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			AnimalResponse animal = animalService().buscarPorIdAutorizado(id, principal);
			model.addAttribute("pageTitle", "Editar animal");
			model.addAttribute("animalId", id);
			model.addAttribute("animal", editarAnimalRequest(animal));
			adicionarOpcoesAnimal(model);
			return "admin/animais/editar";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/admin/animais";
		}
	}

	@PostMapping("/admin/animais/{id}/editar")
	public String atualizarAnimal(
			@PathVariable Long id,
			@Valid @ModelAttribute("animal") AnimalRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Editar animal");
		model.addAttribute("animalId", id);
		if (bindingResult.hasErrors()) {
			adicionarOpcoesAnimal(model);
			return "admin/animais/editar";
		}
		try {
			animalService().atualizar(id, request, principal);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("animal.invalido", ex.getMessage());
			adicionarOpcoesAnimal(model);
			return "admin/animais/editar";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Animal atualizado com sucesso.");
		return "redirect:/admin/animais";
	}

	@PostMapping("/admin/animais/{id}/desativar")
	public String desativarAnimal(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		try {
			animalService().excluir(id, principal);
			redirectAttributes.addFlashAttribute("sucesso", "Animal desativado com sucesso.");
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/admin/animais";
	}

	@GetMapping("/admin/consultas")
	public String listarConsultas(
			@RequestParam(defaultValue = "0") int page,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("dataHora").descending().and(Sort.by("id").descending()));
		model.addAttribute("pageTitle", "Consultas");
		model.addAttribute("consultas", listarConsultas(pageable, principal));
		return "admin/consultas/lista";
	}

	@GetMapping("/admin/consultas/{id}")
	public String detalheConsulta(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			model.addAttribute("pageTitle", "Consulta");
			model.addAttribute("consulta", consultaService().buscarPorIdAutorizado(id, principal));
			return "admin/consultas/detalhe";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/admin/consultas";
		}
	}

	@GetMapping("/admin/prescricoes")
	public String listarPrescricoes(
			@RequestParam(defaultValue = "0") int page,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("id").descending());
		model.addAttribute("pageTitle", "Prescrições");
		model.addAttribute("prescricoes", listarPrescricoes(pageable, principal));
		return "admin/prescricoes/lista";
	}

	@GetMapping("/admin/prescricoes/{id}")
	public String detalhePrescricao(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			model.addAttribute("pageTitle", "Prescrição");
			model.addAttribute("prescricao", prescricaoService().buscarPorIdAutorizado(id, principal));
			return "admin/prescricoes/detalhe";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/admin/prescricoes";
		}
	}

	@GetMapping("/admin/adesoes")
	public String listarAdesoes(
			@RequestParam(defaultValue = "0") int page,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("dataRegistro").descending().and(Sort.by("id").descending()));
		model.addAttribute("pageTitle", "Adesão");
		model.addAttribute("adesoes", listarAdesoes(pageable, principal));
		return "admin/adesoes/lista";
	}

	@GetMapping("/admin/adesoes/{id}")
	public String detalheAdesao(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			model.addAttribute("pageTitle", "Adesão");
			model.addAttribute("adesao", adesaoPrescricaoService().buscarPorIdAutorizado(id, principal));
			return "admin/adesoes/detalhe";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/admin/adesoes";
		}
	}

	private Page<AnimalResponse> listarAnimais(String nome, String ativo, Pageable pageable, UsuarioPrincipal principal) {
		AnimalService service = animalService.getIfAvailable();
		if (service == null || principal == null) {
			return Page.empty(pageable);
		}
		return service.listarAutorizado(nome, null, null, null, ativo, pageable, principal);
	}

	private Page<ConsultaResponse> listarConsultas(Pageable pageable, UsuarioPrincipal principal) {
		ConsultaService service = consultaService.getIfAvailable();
		if (service == null || principal == null) {
			return Page.empty(pageable);
		}
		return service.listarAutorizado(null, null, null, null, null, pageable, principal);
	}

	private Page<PrescricaoResponse> listarPrescricoes(Pageable pageable, UsuarioPrincipal principal) {
		PrescricaoService service = prescricaoService.getIfAvailable();
		if (service == null || principal == null) {
			return Page.empty(pageable);
		}
		return service.listarAutorizado(null, null, pageable, principal);
	}

	private Page<AdesaoPrescricaoResponse> listarAdesoes(Pageable pageable, UsuarioPrincipal principal) {
		AdesaoPrescricaoService service = adesaoPrescricaoService.getIfAvailable();
		if (service == null || principal == null) {
			return Page.empty(pageable);
		}
		return service.listarAutorizado(null, null, null, null, pageable, principal);
	}

	private void adicionarOpcoesAnimal(Model model) {
		model.addAttribute("especies", especies());
		model.addAttribute("racas", racas());
	}

	private List<EspecieResponse> especies() {
		EspecieService service = especieService.getIfAvailable();
		if (service == null) {
			return List.of();
		}
		return service.listar(null, PageRequest.of(0, OPTION_SIZE, Sort.by("nome").ascending())).getContent();
	}

	private List<RacaResponse> racas() {
		RacaService service = racaService.getIfAvailable();
		if (service == null) {
			return List.of();
		}
		return service.listar(null, null, PageRequest.of(0, OPTION_SIZE, Sort.by("nome").ascending())).getContent();
	}

	private AnimalRequest novoAnimalRequest() {
		return new AnimalRequest("", null, null, null, null, null, null);
	}

	private AnimalRequest editarAnimalRequest(AnimalResponse animal) {
		return new AnimalRequest(
				animal.nome(),
				animal.especieId(),
				animal.racaId(),
				animal.sexo(),
				animal.castrado(),
				animal.clinicaId(),
				animal.ativo()
		);
	}

	private AnimalService animalService() {
		AnimalService service = animalService.getIfAvailable();
		if (service == null) {
			throw new AccessDeniedException("Servico de animais indisponivel.");
		}
		return service;
	}

	private ConsultaService consultaService() {
		ConsultaService service = consultaService.getIfAvailable();
		if (service == null) {
			throw new AccessDeniedException("Servico de consultas indisponivel.");
		}
		return service;
	}

	private PrescricaoService prescricaoService() {
		PrescricaoService service = prescricaoService.getIfAvailable();
		if (service == null) {
			throw new AccessDeniedException("Servico de prescricoes indisponivel.");
		}
		return service;
	}

	private AdesaoPrescricaoService adesaoPrescricaoService() {
		AdesaoPrescricaoService service = adesaoPrescricaoService.getIfAvailable();
		if (service == null) {
			throw new AccessDeniedException("Servico de adesoes indisponivel.");
		}
		return service;
	}

}
