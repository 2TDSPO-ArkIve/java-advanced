package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_VS_FEEDBACK_NPS")
public class FeedbackNps {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_FEEDBACK_NPS")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_RESPONSAVEL")
	private Responsavel responsavel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_ANIMAL")
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CLINICA")
	private Clinica clinica;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CONSULTA")
	private Consulta consulta;

	@Column(name = "NR_NOTA", nullable = false)
	private Integer nota;

	@Column(name = "DS_COMENTARIO", length = 1000)
	private String comentario;

	@Column(name = "DT_RESPOSTA", nullable = false)
	private LocalDateTime dataResposta = LocalDateTime.now();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Responsavel getResponsavel() {
		return responsavel;
	}

	public void setResponsavel(Responsavel responsavel) {
		this.responsavel = responsavel;
	}

	public Animal getAnimal() {
		return animal;
	}

	public void setAnimal(Animal animal) {
		this.animal = animal;
	}

	public Clinica getClinica() {
		return clinica;
	}

	public void setClinica(Clinica clinica) {
		this.clinica = clinica;
	}

	public Consulta getConsulta() {
		return consulta;
	}

	public void setConsulta(Consulta consulta) {
		this.consulta = consulta;
	}

	public Integer getNota() {
		return nota;
	}

	public void setNota(Integer nota) {
		this.nota = nota;
	}

	public String getComentario() {
		return comentario;
	}

	public void setComentario(String comentario) {
		this.comentario = comentario;
	}

	public LocalDateTime getDataResposta() {
		return dataResposta;
	}

	public void setDataResposta(LocalDateTime dataResposta) {
		this.dataResposta = dataResposta;
	}

}
