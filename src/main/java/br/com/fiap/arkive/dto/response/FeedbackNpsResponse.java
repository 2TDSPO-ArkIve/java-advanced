package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.FeedbackNps;

import java.time.LocalDateTime;

public record FeedbackNpsResponse(
		Long id,
		Long responsavelId,
		String responsavelNome,
		Long animalId,
		String animalNome,
		Long clinicaId,
		String clinicaNome,
		Long consultaId,
		Integer nota,
		String comentario,
		LocalDateTime dataResposta
) {
	public static FeedbackNpsResponse fromEntity(FeedbackNps feedback) {
		Long responsavelId = feedback.getResponsavel() == null ? null : feedback.getResponsavel().getId();
		String responsavelNome = feedback.getResponsavel() == null ? null : feedback.getResponsavel().getNome();
		Long animalId = feedback.getAnimal() == null ? null : feedback.getAnimal().getId();
		String animalNome = feedback.getAnimal() == null ? null : feedback.getAnimal().getNome();
		Long clinicaId = feedback.getClinica() == null ? null : feedback.getClinica().getId();
		String clinicaNome = feedback.getClinica() == null ? null : feedback.getClinica().getNome();
		Long consultaId = feedback.getConsulta() == null ? null : feedback.getConsulta().getId();
		return new FeedbackNpsResponse(
				feedback.getId(),
				responsavelId,
				responsavelNome,
				animalId,
				animalNome,
				clinicaId,
				clinicaNome,
				consultaId,
				feedback.getNota(),
				feedback.getComentario(),
				feedback.getDataResposta()
		);
	}
}
