package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.RelatoAnimal;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RelatoAnimalResponse(
		Long id,
		Long animalId,
		String animalNome,
		String animalArkivePatientId,
		Long responsavelId,
		String responsavelNome,
		String texto,
		LocalDateTime dataCriacao,
		LocalDate dataOcorrencia,
		boolean originadoDeTranscricao
) {
	public static RelatoAnimalResponse fromEntity(RelatoAnimal relato) {
		return new RelatoAnimalResponse(
				relato.getId(),
				relato.getAnimal().getId(),
				relato.getAnimal().getNome(),
				relato.getAnimal().getArkivePatientId(),
				relato.getAutor().getId(),
				relato.getAutor().getNome(),
				relato.getTexto(),
				relato.getDataCriacao(),
				relato.getDataOcorrencia(),
				"S".equals(relato.getOrigemTranscricao())
		);
	}
}
