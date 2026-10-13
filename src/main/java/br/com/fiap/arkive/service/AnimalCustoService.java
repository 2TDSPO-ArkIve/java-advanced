package br.com.fiap.arkive.service;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.dto.response.AnimalCustoResponse;
import br.com.fiap.arkive.dto.response.AnimalCustoResponse.ConsultaCusto;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository.CustoMedicamentoPorConsulta;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("!local-nodb")
public class AnimalCustoService {

	private static final BigDecimal ZERO_MONETARIO = new BigDecimal("0.00");

	private final AnimalRepository animalRepository;
	private final ConsultaRepository consultaRepository;
	private final PrescricaoRepository prescricaoRepository;
	private final ClinicalAccessService clinicalAccessService;

	public AnimalCustoService(
			AnimalRepository animalRepository,
			ConsultaRepository consultaRepository,
			PrescricaoRepository prescricaoRepository,
			ClinicalAccessService clinicalAccessService
	) {
		this.animalRepository = animalRepository;
		this.consultaRepository = consultaRepository;
		this.prescricaoRepository = prescricaoRepository;
		this.clinicalAccessService = clinicalAccessService;
	}

	@Transactional(readOnly = true)
	public AnimalCustoResponse buscar(Long animalId, UsuarioPrincipal principal) {
		var animal = animalRepository.findById(animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
		clinicalAccessService.exigirLeituraAnimal(principal, animal);

		var consultas = consultaRepository.buscarCustosFinalizadosPorAnimal(animalId).stream()
				.filter(consulta -> StatusConsulta.FI.getCodigo().equals(consulta.getStatus()))
				.filter(consulta -> clinicalAccessService.podeLerConsulta(principal, consulta))
				.sorted(Comparator.comparing(Consulta::getDataHora).reversed()
						.thenComparing(Consulta::getId, Comparator.reverseOrder()))
				.toList();

		Map<Long, CustoMedicamentoPorConsulta> medicamentosPorConsulta = consultas.isEmpty()
				? Map.of()
				: prescricaoRepository.somarCustosMedicamentosFinalizadosPorAnimal(animalId).stream()
						.collect(Collectors.toMap(CustoMedicamentoPorConsulta::getConsultaId, Function.identity()));

		BigDecimal totalConsultas = ZERO_MONETARIO;
		BigDecimal totalMedicamentos = ZERO_MONETARIO;
		long quantidadeConsultasComValor = 0;
		long quantidadePrescricoesComValor = 0;
		var detalhes = new java.util.ArrayList<ConsultaCusto>(consultas.size());

		for (Consulta consulta : consultas) {
			BigDecimal valorConsulta = consulta.getValorConsulta() == null
					? null : monetario(consulta.getValorConsulta());
			CustoMedicamentoPorConsulta agregado = medicamentosPorConsulta.get(consulta.getId());
			BigDecimal valorMedicamentos = agregado == null
					? ZERO_MONETARIO : monetario(agregado.getValorMedicamentos());
			long prescricoesComValor = agregado == null || agregado.getQuantidadePrescricoesComValor() == null
					? 0 : agregado.getQuantidadePrescricoesComValor();

			if (valorConsulta != null) {
				totalConsultas = totalConsultas.add(valorConsulta);
				quantidadeConsultasComValor++;
			}
			totalMedicamentos = totalMedicamentos.add(valorMedicamentos);
			quantidadePrescricoesComValor += prescricoesComValor;

			StatusConsulta status = StatusConsulta.fromCodigo(consulta.getStatus());
			detalhes.add(new ConsultaCusto(
					consulta.getId(),
					consulta.getDataHora(),
					consulta.getStatus(),
					status.getDescricao(),
					valorConsulta,
					valorMedicamentos,
					(valorConsulta == null ? ZERO_MONETARIO : valorConsulta).add(valorMedicamentos)
			));
		}

		return new AnimalCustoResponse(
				animal.getId(),
				animal.getArkivePatientId(),
				animal.getNome(),
				totalConsultas,
				totalMedicamentos,
				totalConsultas.add(totalMedicamentos),
				quantidadeConsultasComValor,
				quantidadePrescricoesComValor,
				detalhes
		);
	}

	private BigDecimal monetario(BigDecimal valor) {
		return valor == null ? ZERO_MONETARIO : valor.setScale(2);
	}
}
