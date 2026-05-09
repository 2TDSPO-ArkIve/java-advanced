package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class AnimalResponsavelId implements Serializable {

	@Column(name = "ID_ANIMAL")
	private Long animalId;

	@Column(name = "ID_RESPONSAVEL")
	private Long responsavelId;

	@Column(name = "DT_INICIO")
	private LocalDate dataInicio;

	public Long getAnimalId() {
		return animalId;
	}

	public void setAnimalId(Long animalId) {
		this.animalId = animalId;
	}

	public Long getResponsavelId() {
		return responsavelId;
	}

	public void setResponsavelId(Long responsavelId) {
		this.responsavelId = responsavelId;
	}

	public LocalDate getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(LocalDate dataInicio) {
		this.dataInicio = dataInicio;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AnimalResponsavelId that)) {
			return false;
		}
		return Objects.equals(animalId, that.animalId)
				&& Objects.equals(responsavelId, that.responsavelId)
				&& Objects.equals(dataInicio, that.dataInicio);
	}

	@Override
	public int hashCode() {
		return Objects.hash(animalId, responsavelId, dataInicio);
	}

}
