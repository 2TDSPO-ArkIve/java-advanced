package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.AnimalResponsavel;

import java.time.LocalDate;

public record AnimalResponsavelResponse(
		Long animalId,
		String animalNome,
		Long responsavelId,
		String responsavelNome,
		String tipoVinculo,
		LocalDate dataInicio,
		LocalDate dataFim,
		String principal,
		String ativo
) {
	public static AnimalResponsavelResponse fromEntity(AnimalResponsavel animalResponsavel) {
		return new AnimalResponsavelResponse(
				animalResponsavel.getAnimal().getId(),
				animalResponsavel.getAnimal().getNome(),
				animalResponsavel.getResponsavel().getId(),
				animalResponsavel.getResponsavel().getNome(),
				animalResponsavel.getTipoVinculo(),
				animalResponsavel.getId().getDataInicio(),
				animalResponsavel.getDataFim(),
				animalResponsavel.getPrincipal(),
				animalResponsavel.getAtivo()
		);
	}
}
