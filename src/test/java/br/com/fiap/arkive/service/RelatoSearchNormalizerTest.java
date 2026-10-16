package br.com.fiap.arkive.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RelatoSearchNormalizerTest {

	private final RelatoSearchNormalizer normalizer = new RelatoSearchNormalizer();

	@Test
	void removeAcentosNormalizaCaixaEColapsaEspacos() {
		assertEquals("VOMITO DURANTE A MADRUGADA", normalizer.normalize("  Vômito   durante à madrugada  "));
	}

	@Test
	void preservaSemanticaSemStemming() {
		String vomito = normalizer.normalize("vômito");
		String vomitar = normalizer.normalize("vomitar");
		assertEquals("VOMITO", vomito);
		assertEquals("VOMITAR", vomitar);
		org.junit.jupiter.api.Assertions.assertFalse(vomitar.contains(vomito));
		org.junit.jupiter.api.Assertions.assertTrue(vomito.contains("VOMIT"));
		org.junit.jupiter.api.Assertions.assertTrue(vomitar.contains("VOMIT"));
	}

	@Test
	void ausenteOuVazioRetornaNull() {
		assertNull(normalizer.normalize(null));
		assertNull(normalizer.normalize("   "));
	}
}
