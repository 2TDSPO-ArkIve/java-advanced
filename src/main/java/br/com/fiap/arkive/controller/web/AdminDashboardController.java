package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AdesaoPrescricaoService;
import br.com.fiap.arkive.service.AnimalService;
import br.com.fiap.arkive.service.ConsultaService;
import br.com.fiap.arkive.service.PrescricaoService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminDashboardController {

	private final ObjectProvider<AnimalService> animalService;
	private final ObjectProvider<ConsultaService> consultaService;
	private final ObjectProvider<PrescricaoService> prescricaoService;
	private final ObjectProvider<AdesaoPrescricaoService> adesaoPrescricaoService;

	public AdminDashboardController(
			ObjectProvider<AnimalService> animalService,
			ObjectProvider<ConsultaService> consultaService,
			ObjectProvider<PrescricaoService> prescricaoService,
			ObjectProvider<AdesaoPrescricaoService> adesaoPrescricaoService
	) {
		this.animalService = animalService;
		this.consultaService = consultaService;
		this.prescricaoService = prescricaoService;
		this.adesaoPrescricaoService = adesaoPrescricaoService;
	}

	@GetMapping("/admin/dashboard")
	public String dashboard(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Visão Geral");
		model.addAttribute("sectionTitle", "Administração da Clínica");
		model.addAttribute("quickAccess", quickAccess(principal(authentication)));
		return "admin/dashboard";
	}

	private List<QuickAccessCard> quickAccess(UsuarioPrincipal principal) {
		return List.of(
				new QuickAccessCard("Animais", "Cadastro e manutenção dos animais da clínica.", "/admin/animais", totalAnimais(principal)),
				new QuickAccessCard("Consultas", "Acompanhamento de consultas no escopo da clínica.", "/admin/consultas", totalConsultas(principal)),
				new QuickAccessCard("Prescrições", "Consulta de prescrições registradas para atendimentos da clínica.", "/admin/prescricoes", totalPrescricoes(principal)),
				new QuickAccessCard("Adesão", "Consulta de registros de adesão terapêutica.", "/admin/adesoes", totalAdesoes(principal))
		);
	}

	private Long totalAnimais(UsuarioPrincipal principal) {
		AnimalService service = animalService.getIfAvailable();
		if (service == null || principal == null) {
			return null;
		}
		try {
			return service.listarAutorizado(null, null, null, null, null, countPage(), principal).getTotalElements();
		} catch (AccessDeniedException ex) {
			return null;
		}
	}

	private Long totalConsultas(UsuarioPrincipal principal) {
		ConsultaService service = consultaService.getIfAvailable();
		if (service == null || principal == null) {
			return null;
		}
		try {
			return service.listarAutorizado(null, null, null, null, null, countPage(), principal).getTotalElements();
		} catch (AccessDeniedException ex) {
			return null;
		}
	}

	private Long totalPrescricoes(UsuarioPrincipal principal) {
		PrescricaoService service = prescricaoService.getIfAvailable();
		if (service == null || principal == null) {
			return null;
		}
		try {
			return service.listarAutorizado(null, null, countPage(), principal).getTotalElements();
		} catch (AccessDeniedException ex) {
			return null;
		}
	}

	private Long totalAdesoes(UsuarioPrincipal principal) {
		AdesaoPrescricaoService service = adesaoPrescricaoService.getIfAvailable();
		if (service == null || principal == null) {
			return null;
		}
		try {
			return service.listarAutorizado(null, null, null, null, countPage(), principal).getTotalElements();
		} catch (AccessDeniedException ex) {
			return null;
		}
	}

	private Pageable countPage() {
		return PageRequest.of(0, 1);
	}

	private UsuarioPrincipal principal(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal) {
			return usuarioPrincipal;
		}
		return null;
	}

	public record QuickAccessCard(String label, String description, String href, Long count) {
	}

}
