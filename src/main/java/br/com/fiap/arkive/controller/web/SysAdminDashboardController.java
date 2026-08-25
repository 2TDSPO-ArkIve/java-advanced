package br.com.fiap.arkive.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SysAdminDashboardController {

	@GetMapping("/sysadmin/dashboard")
	public String dashboard(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Visão Geral");
		model.addAttribute("sectionTitle", "Administração Global");
		return "sysadmin/dashboard";
	}

}
