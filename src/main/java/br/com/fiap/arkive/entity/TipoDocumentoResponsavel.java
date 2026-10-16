package br.com.fiap.arkive.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Document types accepted by public responsible-person self-registration.
 */
public enum TipoDocumentoResponsavel {
	CPF,
	RG;

	@JsonCreator
	public static TipoDocumentoResponsavel fromJson(String valor) {
		if (valor == null) {
			return null;
		}
		try {
			return valueOf(valor.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			throw new IllegalArgumentException("Tipo de documento inválido.");
		}
	}
}
