package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Consulta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultaResponse(
		Long id,
		LocalDateTime dataHora,
		String modalidade,
		String motivo,
		String sintomas,
		String observacao,
		String observacaoResponsavel,
		BigDecimal peso,
		String transcricao,
		String status,
		String statusDescricao,
		Long animalId,
		String animalNome,
		String animalArkivePatientId,
		Long veterinarioId,
		String veterinarioNome,
		Long clinicaId,
		String clinicaNome,
		String endereco,
		BigDecimal valorConsulta
) {
	public ConsultaResponse(Long id, LocalDateTime dataHora, String modalidade, String motivo,
			String sintomas, String observacao, BigDecimal peso, String transcricao, String status,
			String statusDescricao, Long animalId, String animalNome, Long veterinarioId,
			String veterinarioNome, Long clinicaId, String clinicaNome) {
		this(id, dataHora, modalidade, motivo, sintomas, observacao, null, peso, transcricao, status,
				statusDescricao, animalId, animalNome, null, veterinarioId, veterinarioNome,
				clinicaId, clinicaNome, null, null);
	}

	public ConsultaResponse(Long id, LocalDateTime dataHora, String modalidade, String motivo,
			String sintomas, String observacao, BigDecimal peso, String transcricao, String status,
			String statusDescricao, Long animalId, String animalNome, Long veterinarioId,
			String veterinarioNome, Long clinicaId, String clinicaNome, String endereco) {
		this(id, dataHora, modalidade, motivo, sintomas, observacao, null, peso, transcricao, status,
				statusDescricao, animalId, animalNome, null, veterinarioId, veterinarioNome,
				clinicaId, clinicaNome, endereco, null);
	}

	public ConsultaResponse(Long id, LocalDateTime dataHora, String modalidade, String motivo,
			String sintomas, String observacao, BigDecimal peso, String transcricao, String status,
			String statusDescricao, Long animalId, String animalNome, Long veterinarioId,
			String veterinarioNome, Long clinicaId, String clinicaNome, String endereco,
			BigDecimal valorConsulta) {
		this(id, dataHora, modalidade, motivo, sintomas, observacao, null, peso, transcricao, status,
				statusDescricao, animalId, animalNome, null, veterinarioId, veterinarioNome,
				clinicaId, clinicaNome, endereco, valorConsulta);
	}

	public static ConsultaResponse fromEntity(Consulta consulta) {
		Long clinicaId = consulta.getClinica() == null ? null : consulta.getClinica().getId();
		String clinicaNome = consulta.getClinica() == null ? null : consulta.getClinica().getNome();
		StatusConsulta statusConsulta = StatusConsulta.fromCodigo(consulta.getStatus());
		return new ConsultaResponse(
				consulta.getId(), consulta.getDataHora(), consulta.getModalidade(), consulta.getMotivo(),
				consulta.getSintomas(), consulta.getObservacao(), consulta.getObservacaoResponsavel(),
				consulta.getPeso(), consulta.getTranscricao(), consulta.getStatus(), statusConsulta.getDescricao(),
				consulta.getAnimal().getId(), consulta.getAnimal().getNome(), consulta.getAnimal().getArkivePatientId(),
				consulta.getVeterinario().getId(), consulta.getVeterinario().getNome(), clinicaId, clinicaNome,
				consulta.getEndereco(), consulta.getValorConsulta()
		);
	}
}
