package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.TipoDocumentoResponsavel;
import br.com.fiap.arkive.entity.TipoUsuario;

public record RegistroResponsavelResponse(
		Long usuarioId,
		Long responsavelId,
		String nome,
		String email,
		TipoUsuario tipo,
		String ativo,
		boolean trocaSenhaObrigatoria,
		TipoDocumentoResponsavel tipoDocumento
) {
	public RegistroResponsavelResponse(
			Long usuarioId,
			Long responsavelId,
			String nome,
			String email,
			TipoUsuario tipo,
			String ativo,
			boolean trocaSenhaObrigatoria
	) {
		this(usuarioId, responsavelId, nome, email, tipo, ativo, trocaSenhaObrigatoria, null);
	}
}
