package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShareTokenRequest(
		@NotBlank @Size(max = 512) String token
) {
}
