package br.com.fiap.arkive.service;

import br.com.fiap.arkive.domain.consulta.ConsultaStatusTransitionPolicy;
import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.dto.response.ClinicalSupportResponse;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.clinical.ClinicalSupportProviderResult;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class ClinicalSupportPersistenceService {

	private final ConsultaService consultaService;
	private final ConsultaRepository consultaRepository;
	private final DiagnosticoService diagnosticoService;
	private final ClinicalAccessService clinicalAccessService;

	public ClinicalSupportPersistenceService(
			ConsultaService consultaService,
			ConsultaRepository consultaRepository,
			DiagnosticoService diagnosticoService,
			ClinicalAccessService clinicalAccessService
	) {
		this.consultaService = consultaService;
		this.consultaRepository = consultaRepository;
		this.diagnosticoService = diagnosticoService;
		this.clinicalAccessService = clinicalAccessService;
	}

	@Transactional
	public ClinicalSupportResponse persistirSuporte(Long consultaId, ClinicalSupportProviderResult result, UsuarioPrincipal principal) {
		Consulta consulta = consultaService.buscarEntidade(consultaId);
		clinicalAccessService.exigirEscritaClinicaVeterinario(principal, consulta);
		StatusConsulta atual = StatusConsulta.fromCodigo(consulta.getStatus());
		if (atual != StatusConsulta.EP) {
			throw new BusinessException("Consulta mudou de estado antes da persistencia do suporte clinico.", HttpStatus.CONFLICT);
		}
		ConsultaStatusTransitionPolicy.validar(atual, StatusConsulta.AP);
		Diagnostico diagnostico = diagnosticoService.criarSuporteClinicoIa(
				consulta,
				result.diagnostico(),
				result.severidade(),
				result.insightIa(),
				result.confianca()
		);
		consulta.setStatus(StatusConsulta.AP.getCodigo());
		consultaRepository.save(consulta);
		return ClinicalSupportResponse.fromEntities(consulta, diagnostico);
	}
}
