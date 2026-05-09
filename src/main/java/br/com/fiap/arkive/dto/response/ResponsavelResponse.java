package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Responsavel;

import java.time.LocalDate;

public record ResponsavelResponse(
		Long id,
		String nome,
		String documento,
		String email,
		String telefone,
		String tipo,
		LocalDate dataCadastro,
		String notificacao,
		String ativo
) {
	public static ResponsavelResponse fromEntity(Responsavel responsavel) {
		return new ResponsavelResponse(
				responsavel.getId(),
				responsavel.getNome(),
				responsavel.getDocumento(),
				responsavel.getEmail(),
				responsavel.getTelefone(),
				responsavel.getTipo(),
				responsavel.getDataCadastro(),
				responsavel.getNotificacao(),
				responsavel.getAtivo()
		);
	}
}
