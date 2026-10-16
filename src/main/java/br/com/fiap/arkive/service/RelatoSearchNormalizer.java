package br.com.fiap.arkive.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class RelatoSearchNormalizer {

	public String normalize(String value) {
		if (value == null) {
			return null;
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "")
				.toUpperCase(Locale.ROOT)
				.trim()
				.replaceAll("\\s+", " ");
		return normalized.isEmpty() ? null : normalized;
	}
}
