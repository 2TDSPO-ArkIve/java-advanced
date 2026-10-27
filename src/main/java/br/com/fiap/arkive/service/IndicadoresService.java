package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.AnimalConsultasIndicadorResponse;
import br.com.fiap.arkive.dto.response.PacientesAtivosIndicadorResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.IndicadoresOracleRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class IndicadoresService {

	private final AnimalRepository animalRepository;
	private final ClinicalAccessService clinicalAccessService;
	private final IndicadoresOracleRepository indicadoresOracleRepository;

	public IndicadoresService(
			AnimalRepository animalRepository,
			ClinicalAccessService clinicalAccessService,
			IndicadoresOracleRepository indicadoresOracleRepository
	) {
		this.animalRepository = animalRepository;
		this.clinicalAccessService = clinicalAccessService;
		this.indicadoresOracleRepository = indicadoresOracleRepository;
	}

	@Transactional(readOnly = true)
	public AnimalConsultasIndicadorResponse buscarConsultasAnimal(
			Long animalId, UsuarioPrincipal principal
	) {
		var animal = animalRepository.findById(animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
		clinicalAccessService.exigirLeituraAnimal(principal, animal);

		long totalConsultas = indicadoresOracleRepository.totalConsultasAnimal(animalId);
		long totalFinalizadas = indicadoresOracleRepository.totalConsultasFinalizadas(animalId);
		return new AnimalConsultasIndicadorResponse(animalId, totalConsultas, totalFinalizadas);
	}

	@Transactional(readOnly = true)
	public PacientesAtivosIndicadorResponse buscarPacientesAtivos(UsuarioPrincipal principal) {
		if (principal == null || principal.getTipoUsuario() != TipoUsuario.SYSADMIN) {
			throw new AccessDeniedException("Indicador global permitido apenas ao SysAdmin.");
		}
		return new PacientesAtivosIndicadorResponse(
				indicadoresOracleRepository.totalPacientesAtivos());
	}
}
