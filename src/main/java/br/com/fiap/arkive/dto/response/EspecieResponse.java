package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Especie;

public record EspecieResponse(
		Long id,
		String nome,
		String ativo
) {
	public static EspecieResponse fromEntity(Especie especie) {
		return new EspecieResponse(especie.getId(), especie.getNome(), especie.getAtivo());
	}
}
