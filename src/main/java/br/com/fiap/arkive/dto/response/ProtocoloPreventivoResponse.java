package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.ProtocoloPreventivo;

public record ProtocoloPreventivoResponse(
		Long id,
		String nome,
		String tipo,
		String descricao,
		Integer intervalo,
		Integer idadeMin,
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
				protocolo.getIntervalo(),
				protocolo.getIdadeMin(),
				especieId,
				especieNome,
				racaId,
				racaNome,
				protocolo.getAtivo()
		);
	}
}
