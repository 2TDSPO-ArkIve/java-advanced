package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Clinica;

public record ClinicaResponse(
		Long id,
		String nome,
		String cnpj,
		String endereco,
		String telefone,
		String email,
		String ativo
) {
	public static ClinicaResponse fromEntity(Clinica clinica) {
		return new ClinicaResponse(
				clinica.getId(),
				clinica.getNome(),
				clinica.getCnpj(),
				clinica.getEndereco(),
				clinica.getTelefone(),
				clinica.getEmail(),
				clinica.getAtivo()
		);
	}
}
