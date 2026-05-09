package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.ProtocoloPreventivo;

public record ProtocoloPreventivoResponse(
		Long id,
		String nome,
		String tipo,
		String descricao,
		Integer intervaloDias,
		Integer idadeMinMeses,
		Long especieId,
		String especieNome,
		Long racaId,
		String racaNome,
		String ativo
) {
	public static ProtocoloPreventivoResponse fromEntity(ProtocoloPreventivo protocolo) {
		Long especieId = protocolo.getEspecie() == null ? null : protocolo.getEspecie().getId();
		String especieNome = protocolo.getEspecie() == null ? null : protocolo.getEspecie().getNome();
		Long racaId = protocolo.getRaca() == null ? null : protocolo.getRaca().getId();
		String racaNome = protocolo.getRaca() == null ? null : protocolo.getRaca().getNome();
		return new ProtocoloPreventivoResponse(
				protocolo.getId(),
				protocolo.getNome(),
				protocolo.getTipo(),
				protocolo.getDescricao(),
				protocolo.getIntervaloDias(),
				protocolo.getIdadeMinMeses(),
				especieId,
				especieNome,
				racaId,
				racaNome,
				protocolo.getAtivo()
		);
	}
}
