package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Veterinario;

public record VeterinarioResponse(
		Long id,
		String nome,
		String crmv,
		String especialidade,
		String email,
		Long clinicaId,
		String clinicaNome,
		String ativo
) {
	public static VeterinarioResponse fromEntity(Veterinario veterinario) {
		Long clinicaId = veterinario.getClinica() == null ? null : veterinario.getClinica().getId();
		String clinicaNome = veterinario.getClinica() == null ? null : veterinario.getClinica().getNome();
		return new VeterinarioResponse(
				veterinario.getId(),
				veterinario.getNome(),
				veterinario.getCrmv(),
				veterinario.getEspecialidade(),
				veterinario.getEmail(),
				clinicaId,
				clinicaNome,
				veterinario.getAtivo()
		);
	}
}
