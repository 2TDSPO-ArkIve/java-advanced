package br.com.fiap.arkive.dto.request;

import br.com.fiap.arkive.entity.PlataformaDispositivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarDispositivoRequest(
		@NotBlank
		@Size(max = 255)
		@Pattern(regexp = "^(ExponentPushToken|ExpoPushToken)\\[[A-Za-z0-9_-]+]$", message = "deve ser um Expo push token valido")
		String expoPushToken,

		@NotNull
		PlataformaDispositivo platform,

		@NotBlank
		@Size(max = 100)
		String installationId
) {
}

