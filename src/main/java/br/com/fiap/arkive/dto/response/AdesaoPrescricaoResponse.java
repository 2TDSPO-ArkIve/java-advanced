package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.AdesaoPrescricao;

import java.time.LocalDateTime;

public record AdesaoPrescricaoResponse(
		Long id,
		Long prescricaoId,
		Long responsavelId,
		String responsavelNome,
		Long animalId,
		String animalNome,
		String animalArkivePatientId,
		LocalDateTime dataRegistro,
		String tomou,
		String observacao
) {
	public AdesaoPrescricaoResponse(Long id, Long prescricaoId, Long responsavelId, String responsavelNome,
			Long animalId, String animalNome, LocalDateTime dataRegistro, String tomou, String observacao) {
		this(id, prescricaoId, responsavelId, responsavelNome, animalId, animalNome, null,
				dataRegistro, tomou, observacao);
	}

	public static AdesaoPrescricaoResponse fromEntity(AdesaoPrescricao adesao) {
		Long responsavelId = adesao.getResponsavel() == null ? null : adesao.getResponsavel().getId();
		String responsavelNome = adesao.getResponsavel() == null ? null : adesao.getResponsavel().getNome();
		return new AdesaoPrescricaoResponse(
				adesao.getId(),
				adesao.getPrescricao().getId(),
				responsavelId,
				responsavelNome,
				adesao.getAnimal().getId(),
				adesao.getAnimal().getNome(),
				adesao.getAnimal().getArkivePatientId(),
				adesao.getDataRegistro(),
				adesao.getTomou(),
				adesao.getObservacao()
		);
	}
}
