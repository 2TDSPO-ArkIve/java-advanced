package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.entity.Animal;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArkivePatientIdGeneratorTest {

	@Test
	void generatesOpaqueUniqueReadable128BitIdentifiers() {
		ArkivePatientIdGenerator generator = new ArkivePatientIdGenerator();
		Set<String> ids = new HashSet<>();
		for (int i = 0; i < 2_000; i++) {
			String id = generator.generate();
			assertTrue(id.matches("ARK-PET-[2-9A-HJ-NP-Z]{5}(?:-[2-9A-HJ-NP-Z]{5}){3}-[2-9A-HJ-NP-Z]{6}"));
			assertTrue(ids.add(id));
		}
	}

	@Test
	void animalPatientIdIsImmutableAndAbsentFromRequestContract() {
		Animal animal = new Animal();
		animal.assignArkivePatientId("ARK-PET-22222-33333-44444-55555-666666");
		assertThrows(IllegalStateException.class,
				() -> animal.assignArkivePatientId("ARK-PET-AAAAA-BBBBB-CCCCC-DDDDD-EEEEEE"));
		assertFalse(Set.of(AnimalRequest.class.getRecordComponents()).stream()
				.anyMatch(component -> component.getName().equals("arkivePatientId")));
	}
}
