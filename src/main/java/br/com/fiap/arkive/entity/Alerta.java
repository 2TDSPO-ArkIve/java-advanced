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

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ARKIVE_ALERTA")
public class Alerta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_ALERTA")
	private Long id;

	@Column(name = "TP_ALERTA", nullable = false, length = 50)
	private String tipo;

	@Lob
	@Column(name = "DS_MENSAGEM", nullable = false)
	private String mensagem;

	@Column(name = "DT_ENVIO", nullable = false)
	private LocalDateTime dataEnvio = LocalDateTime.now();

	@Column(name = "DT_LEITURA")
	private LocalDateTime dataLeitura;

	@Column(name = "ST_STATUS", nullable = false, length = 20)
	private String status = "ENVIADO";

	@Column(name = "TP_CANAL", nullable = false, length = 20)
	private String canal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_RESPONSAVEL")
	private Responsavel responsavel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CLINICA")
	private Clinica clinica;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_EVENTO_PREV")
	private EventoPreventivo eventoPreventivo;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}

	public LocalDateTime getDataEnvio() {
		return dataEnvio;
	}

	public void setDataEnvio(LocalDateTime dataEnvio) {
		this.dataEnvio = dataEnvio;
	}

	public LocalDateTime getDataLeitura() {
		return dataLeitura;
	}

	public void setDataLeitura(LocalDateTime dataLeitura) {
		this.dataLeitura = dataLeitura;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCanal() {
		return canal;
	}

	public void setCanal(String canal) {
		this.canal = canal;
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

	public Clinica getClinica() {
		return clinica;
	}

	public void setClinica(Clinica clinica) {
		this.clinica = clinica;
	}

	public EventoPreventivo getEventoPreventivo() {
		return eventoPreventivo;
	}

	public void setEventoPreventivo(EventoPreventivo eventoPreventivo) {
		this.eventoPreventivo = eventoPreventivo;
	}

}
