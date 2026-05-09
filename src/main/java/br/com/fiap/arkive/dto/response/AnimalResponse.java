package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Animal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnimalResponse(
		Long id,
		String nome,
		LocalDate dataNascimento,
		BigDecimal pesoKg,
		Long especieId,
		String especieNome,
		Long racaId,
		String racaNome,
		Long clinicaCadastroId,
		String clinicaCadastroNome,
		String ativo
) {
	public static AnimalResponse fromEntity(Animal animal) {
		Long racaId = animal.getRaca() == null ? null : animal.getRaca().getId();
		String racaNome = animal.getRaca() == null ? null : animal.getRaca().getNome();
		Long clinicaId = animal.getClinicaCadastro() == null ? null : animal.getClinicaCadastro().getId();
		String clinicaNome = animal.getClinicaCadastro() == null ? null : animal.getClinicaCadastro().getNome();
		return new AnimalResponse(
				animal.getId(),
				animal.getNome(),
				animal.getDataNascimento(),
				animal.getPesoKg(),
				animal.getEspecie().getId(),
				animal.getEspecie().getNome(),
				racaId,
				racaNome,
				clinicaId,
				clinicaNome,
				animal.getAtivo()
		);
	}
}
