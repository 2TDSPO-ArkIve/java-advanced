package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "TB_ARKIVE_RESPONSAVEL_ANIMAL")
public class AnimalResponsavel {

	@EmbeddedId
	private AnimalResponsavelId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("animalId")
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("responsavelId")
	@JoinColumn(name = "ID_RESPONSAVEL", nullable = false)
	private Responsavel responsavel;

	@Column(name = "TP_VINCULO", nullable = false, length = 40)
	private String tipoVinculo;

	@Column(name = "DT_FIM")
	private LocalDate dataFim;

	@Column(name = "ST_PRINCIPAL", nullable = false, length = 1)
	private String principal = "N";

	@Column(name = "ST_ATIVO", nullable = false, length = 1)
	private String ativo = "S";

	public AnimalResponsavelId getId() {
		return id;
	}

	public void setId(AnimalResponsavelId id) {
		this.id = id;
	}

	public Animal getAnimal() {
		return animal;
	}

	public void setAnimal(Animal animal) {
		this.animal = animal;
	}

	public Responsavel getResponsavel() {
		return responsavel;
	}

	public void setResponsavel(Responsavel responsavel) {
		this.responsavel = responsavel;
	}

	public String getTipoVinculo() {
		return tipoVinculo;
	}

	public void setTipoVinculo(String tipoVinculo) {
		this.tipoVinculo = tipoVinculo;
	}

	public LocalDate getDataFim() {
		return dataFim;
	}

	public void setDataFim(LocalDate dataFim) {
		this.dataFim = dataFim;
	}

	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	public String getAtivo() {
		return ativo;
	}

	public void setAtivo(String ativo) {
		this.ativo = ativo;
	}

}
