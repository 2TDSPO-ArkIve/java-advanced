package br.com.fiap.arkive.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalRepositorySearchContractTest {

	@Test
	void allListingQueriesSearchNameOrArkiveIdCaseInsensitively() {
		assertUnifiedSearch("buscar");
		assertUnifiedSearch("buscarParaResponsavel");
		assertUnifiedSearch("buscarParaVeterinario");
		assertUnifiedSearch("buscarAtivosParaClinica");
	}

	@Test
	void restrictedSearchQueriesKeepTheirAuthorizationPredicates() {
		String responsavel = query("buscarParaResponsavel");
		assertTrue(responsavel.contains("ar.responsavel.id = :responsavelId"));
		assertTrue(responsavel.contains("ar.ativo = 'S'"));

		String veterinario = query("buscarParaVeterinario");
		assertTrue(veterinario.contains(":veterinarioId is not null"));
		assertTrue(veterinario.contains("pa.usuario.veterinario.id = :veterinarioId"));

		String clinica = query("buscarAtivosParaClinica");
		assertTrue(clinica.contains("a.clinica.id = :clinicaId"));
	}

	private void assertUnifiedSearch(String methodName) {
		String query = query(methodName);
		assertTrue(query.contains("lower(a.nome) like lower(concat('%', :busca, '%'))"));
		assertTrue(query.contains("lower(a.arkivePatientId) like lower(concat('%', :busca, '%'))"));
	}

	private String query(String methodName) {
		Method method = Arrays.stream(AnimalRepository.class.getDeclaredMethods())
				.filter(candidate -> candidate.getName().equals(methodName))
				.findFirst()
				.orElseThrow();
		return method.getAnnotation(Query.class).value();
	}
}
