package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_VS_CONSULTA")
public class Consulta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_CONSULTA")
	private Long id;

	@Column(name = "DT_HORA", nullable = false)
	private LocalDateTime dataHora;

	@Column(name = "TP_MODALIDADE", nullable = false, length = 20)
	private String modalidade;

	@Column(name = "DS_MOTIVO", nullable = false, length = 300)
	private String motivo;

	@Column(name = "DS_SINTOMAS_RELATADOS", length = 1000)
	private String sintomasRelatados;

	@Column(name = "DS_OBSERVACOES", length = 2000)
	private String observacoes;

	@Column(name = "NR_PESO_NA_CONSULTA", precision = 5, scale = 2)
	private BigDecimal pesoNaConsulta;

	@Lob
	@Column(name = "DS_TRANSCRICAO_RAW")
	private String transcricaoRaw;

	@Column(name = "ST_STATUS", nullable = false, length = 2)
	private String status = "AG";

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_VETERINARIO", nullable = false)
	private Veterinario veterinario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CLINICA")
	private Clinica clinica;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getDataHora() {
		return dataHora;
	}

	public void setDataHora(LocalDateTime dataHora) {
		this.dataHora = dataHora;
	}

	public String getModalidade() {
		return modalidade;
	}

	public void setModalidade(String modalidade) {
		this.modalidade = modalidade;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	public String getSintomasRelatados() {
		return sintomasRelatados;
	}

	public void setSintomasRelatados(String sintomasRelatados) {
		this.sintomasRelatados = sintomasRelatados;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	public BigDecimal getPesoNaConsulta() {
		return pesoNaConsulta;
	}

	public void setPesoNaConsulta(BigDecimal pesoNaConsulta) {
		this.pesoNaConsulta = pesoNaConsulta;
	}

	public String getTranscricaoRaw() {
		return transcricaoRaw;
	}

	public void setTranscricaoRaw(String transcricaoRaw) {
		this.transcricaoRaw = transcricaoRaw;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Animal getAnimal() {
		return animal;
	}

	public void setAnimal(Animal animal) {
		this.animal = animal;
	}

	public Veterinario getVeterinario() {
		return veterinario;
	}

	public void setVeterinario(Veterinario veterinario) {
		this.veterinario = veterinario;
	}

	public Clinica getClinica() {
		return clinica;
	}

	public void setClinica(Clinica clinica) {
		this.clinica = clinica;
	}

}
