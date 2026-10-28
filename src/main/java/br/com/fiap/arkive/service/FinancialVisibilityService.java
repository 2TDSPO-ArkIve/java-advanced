package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Profile("!local-nodb")
public class FinancialVisibilityService {

	public boolean podeVerCustosConsulta(UsuarioPrincipal principal, Consulta consulta) {
		if (principal == null || principal.getTipoUsuario() == null || consulta == null) {
			return false;
		}
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> true;
			case RESPONSAVEL -> true;
			case VETERINARIO -> principal.getVeterinarioId() != null
					&& consulta.getVeterinario() != null
					&& Objects.equals(principal.getVeterinarioId(), consulta.getVeterinario().getId());
			case ADMIN_CLINICA -> principal.getClinicaId() != null
					&& consulta.getClinica() != null
					&& Objects.equals(principal.getClinicaId(), consulta.getClinica().getId());
		};
	}
}
