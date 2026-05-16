package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Raca;

public record RacaResponse(
		Long id,
		String nome,
		String porte,
		Long especieId,
		String especieNome,
		String ativo
) {
	public static RacaResponse fromEntity(Raca raca) {
		return new RacaResponse(
				raca.getId(),
				raca.getNome(),
				raca.getPorte(),
				raca.getEspecie().getId(),
				raca.getEspecie().getNome(),
				raca.getAtivo()
		);
	}
}
