package br.com.fiap.arkive.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Authentication authentication) {
		if (WebModelSupport.hasRole(authentication, "ROLE_SYSADMIN")) {
			return "redirect:/sysadmin/dashboard";
		}
		if (WebModelSupport.hasRole(authentication, "ROLE_ADMIN_CLINICA")) {
			return "redirect:/admin/dashboard";
		}
		return "redirect:/acesso-web-restrito";
	}

}
