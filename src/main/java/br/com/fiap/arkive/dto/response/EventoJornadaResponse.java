package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.EventoJornada;

import java.time.LocalDateTime;

public record EventoJornadaResponse(
		Long id,
		String tipoEvento,
		LocalDateTime dataEvento,
		String origem,
		String ator,
		Long responsavelId,
		String responsavelNome,
		Long veterinarioId,
		String veterinarioNome,
		Long animalId,
		String animalNome,
		Long clinicaId,
		String clinicaNome,
		String canal,
		String contexto,
		String payloadJson
) {
	public static EventoJornadaResponse fromEntity(EventoJornada evento) {
		Long responsavelId = evento.getResponsavel() == null ? null : evento.getResponsavel().getId();
		String responsavelNome = evento.getResponsavel() == null ? null : evento.getResponsavel().getNome();
		Long veterinarioId = evento.getVeterinario() == null ? null : evento.getVeterinario().getId();
		String veterinarioNome = evento.getVeterinario() == null ? null : evento.getVeterinario().getNome();
		Long animalId = evento.getAnimal() == null ? null : evento.getAnimal().getId();
		String animalNome = evento.getAnimal() == null ? null : evento.getAnimal().getNome();
		Long clinicaId = evento.getClinica() == null ? null : evento.getClinica().getId();
		String clinicaNome = evento.getClinica() == null ? null : evento.getClinica().getNome();
		return new EventoJornadaResponse(
				evento.getId(),
				evento.getTipoEvento(),
				evento.getDataEvento(),
				evento.getOrigem(),
				evento.getAtor(),
				responsavelId,
				responsavelNome,
				veterinarioId,
				veterinarioNome,
				animalId,
				animalNome,
				clinicaId,
				clinicaNome,
				evento.getCanal(),
				evento.getContexto(),
				evento.getPayloadJson()
		);
	}
}
