package br.com.fiap.arkive.service;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.dto.response.AnimalCustoResponse;
import br.com.fiap.arkive.dto.response.AnimalCustoResponse.ConsultaCusto;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@Profile("!local-nodb")
public class AnimalCustoService {

	private final AnimalRepository animalRepository;
	private final ConsultaRepository consultaRepository;
	private final PrescricaoRepository prescricaoRepository;
	private final ClinicalAccessService clinicalAccessService;
	private final FinancialVisibilityService financialVisibilityService;

	public AnimalCustoService(
			AnimalRepository animalRepository,
			ConsultaRepository consultaRepository,
			PrescricaoRepository prescricaoRepository,
			ClinicalAccessService clinicalAccessService,
			FinancialVisibilityService financialVisibilityService
	) {
		this.animalRepository = animalRepository;
		this.consultaRepository = consultaRepository;
		this.prescricaoRepository = prescricaoRepository;
		this.clinicalAccessService = clinicalAccessService;
		this.financialVisibilityService = financialVisibilityService;
	}

	@Transactional(readOnly = true)
	public AnimalCustoResponse buscar(Long animalId, UsuarioPrincipal principal) {
		var animal = animalRepository.findById(animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
		clinicalAccessService.exigirLeituraAnimal(principal, animal);

		boolean veterinario = principal.getTipoUsuario() == br.com.fiap.arkive.entity.TipoUsuario.VETERINARIO;
		Long veterinarioId = principal.getVeterinarioId();
		if (veterinario && veterinarioId == null) {
			throw new AccessDeniedException("Perfil veterinario autenticado sem vinculo valido.");
		}
		if (veterinario && veterinarioId == null) {
			throw new AccessDeniedException("Perfil veterinario autenticado sem vinculo valido.");
		}
		var consultasEncontradas = veterinario
				? consultaRepository.buscarCustosFinalizadosPorAnimalEVeterinario(animalId, veterinarioId)
				: consultaRepository.buscarCustosFinalizadosPorAnimal(animalId);
		var consultas = consultasEncontradas.stream()
				.filter(consulta -> StatusConsulta.FI.getCodigo().equals(consulta.getStatus()))
				.filter(consulta -> clinicalAccessService.podeLerConsulta(principal, consulta))
				.filter(consulta -> financialVisibilityService.podeVerCustosConsulta(principal, consulta))
				.sorted(Comparator.comparing(Consulta::getDataHora).reversed()
						.thenComparing(Consulta::getId, Comparator.reverseOrder()))
				.toList();

		var agregados = consultas.isEmpty()
				? java.util.List.<PrescricaoRepository.CustoMedicamentoPorConsulta>of()
				: veterinario
						? prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimalEVeterinario(
								animalId, veterinarioId)
						: prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(animalId);
		var resultado = CustoAssistencialCalculator.calcular(consultas, agregados);
		var detalhes = resultado.linhas().stream().map(linha -> {
			Consulta consulta = linha.consulta();
			StatusConsulta status = StatusConsulta.fromCodigo(consulta.getStatus());
			return new ConsultaCusto(
					consulta.getId(), consulta.getDataHora(), consulta.getStatus(), status.getDescricao(),
					linha.valorConsulta(), linha.valorMedicamentos(), linha.valorTotal());
		}).toList();

		return new AnimalCustoResponse(
				animal.getId(),
				animal.getArkivePatientId(),
				animal.getNome(),
				resultado.totalConsultas(),
				resultado.totalMedicamentos(),
				resultado.totalGeral(),
				resultado.quantidadeConsultasComValor(),
				resultado.quantidadePrescricoesComValor(),
				detalhes
		);
	}
}
