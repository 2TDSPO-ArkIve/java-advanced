package br.com.fiap.arkive.dto.response;

public record AnimalConsultasIndicadorResponse(
		Long animalId,
		long totalConsultas,
		long totalConsultasFinalizadas
) {
}
