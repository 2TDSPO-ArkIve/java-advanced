package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.VeterinarioCustoResponse;
import br.com.fiap.arkive.dto.response.VeterinarioCustoResponse.ConsultaCusto;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class VeterinarioCustoService {

	private final VeterinarioRepository veterinarioRepository;
	private final ConsultaRepository consultaRepository;
	private final PrescricaoRepository prescricaoRepository;

	public VeterinarioCustoService(
			VeterinarioRepository veterinarioRepository,
			ConsultaRepository consultaRepository,
			PrescricaoRepository prescricaoRepository
	) {
		this.veterinarioRepository = veterinarioRepository;
		this.consultaRepository = consultaRepository;
		this.prescricaoRepository = prescricaoRepository;
	}

	@Transactional(readOnly = true)
	public VeterinarioCustoResponse buscarCustosAutenticados(UsuarioPrincipal principal) {
		if (principal == null || principal.getTipoUsuario() != TipoUsuario.VETERINARIO
				|| principal.getVeterinarioId() == null) {
			throw new AccessDeniedException("Dashboard financeiro permitido apenas ao veterinario autenticado.");
		}
		Long veterinarioId = principal.getVeterinarioId();
		var veterinario = veterinarioRepository.findById(veterinarioId)
				.orElseThrow(() -> new AccessDeniedException("Perfil veterinario autenticado sem vinculo valido."));
		var consultas = consultaRepository.buscarCustosFinalizadosPorVeterinario(veterinarioId);
		var agregados = consultas.isEmpty()
				? java.util.List.<PrescricaoRepository.CustoMedicamentoPorConsulta>of()
				: prescricaoRepository.somarCustosMedicamentosFinalizadosPorVeterinario(veterinarioId);
		var resultado = CustoAssistencialCalculator.calcular(consultas, agregados);
		var detalhes = resultado.linhas().stream().map(linha -> {
			var consulta = linha.consulta();
			var animal = consulta.getAnimal();
			if (animal == null) {
				throw new ResourceNotFoundException("Animal da consulta nao encontrado.");
			}
			return new ConsultaCusto(
					consulta.getId(),
					consulta.getDataHora(),
					animal.getId(),
					animal.getNome(),
					animal.getArkivePatientId(),
					linha.valorConsulta(),
					linha.valorMedicamentos(),
					linha.valorTotal()
			);
		}).toList();

		return new VeterinarioCustoResponse(
				veterinario.getId(),
				veterinario.getNome(),
				resultado.totalConsultas(),
				resultado.totalMedicamentos(),
				resultado.totalGeral(),
				resultado.quantidadeConsultasComValor(),
				resultado.quantidadePrescricoesComValor(),
				detalhes
		);
	}
}
