package br.com.fiap.arkive.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
		assertFalse(responsavel.contains("select distinct a"));
		assertFalse(responsavel.contains("join AnimalResponsavel"));
		assertTrue(responsavel.contains("select ar from AnimalResponsavel ar"));
		assertTrue(responsavel.contains("where ar.animal = a"));
		assertTrue(responsavel.contains("ar.responsavel.id = :responsavelId"));
		assertTrue(responsavel.contains("ar.ativo = 'S'"));
		assertTrue(responsavel.contains("ar.id.dataInicio <= :dataAtual"));
		assertTrue(responsavel.contains("ar.dataFim is null or ar.dataFim >= :dataAtual"));
		assertTrue(responsavel.contains("select pa from PatientAccess pa"));
		assertTrue(responsavel.contains("pa.accessRole = br.com.fiap.arkive.entity.PatientAccessRole.RESPONSAVEL"));
		assertTrue(responsavel.contains("pa.revokedAt is null"));
		assertTrue(responsavel.contains("pa.expiresAt is null or pa.expiresAt > current_timestamp"));

		String veterinario = query("buscarParaVeterinario");
		assertFalse(veterinario.contains("select distinct a"));
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
