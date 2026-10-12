package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.PlataformaDispositivo;
import br.com.fiap.arkive.entity.UsuarioDispositivo;

import java.time.LocalDateTime;

public record UsuarioDispositivoResponse(
		Long id,
		PlataformaDispositivo platform,
		String installationId,
		LocalDateTime createdAt,
		LocalDateTime lastSeenAt,
		boolean active,
		LocalDateTime revokedAt
) {
	public static UsuarioDispositivoResponse fromEntity(UsuarioDispositivo dispositivo) {
		return new UsuarioDispositivoResponse(
				dispositivo.getId(),
				dispositivo.getPlataforma(),
				dispositivo.getInstalacaoId(),
				dispositivo.getDataCriacao(),
				dispositivo.getDataUltimoContato(),
				"S".equals(dispositivo.getAtivo()),
				dispositivo.getDataRevogacao()
		);
	}
}

