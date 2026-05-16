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
@Table(name = "TB_ARKIVE_EVENTO_JORNADA")
public class EventoJornada {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_EVENTO_JORN")
	private Long id;

	@Column(name = "TP_EVENTO", nullable = false, length = 50)
	private String tipoEvento;

	@Column(name = "DT_EVENTO", nullable = false)
	private LocalDateTime dataEvento = LocalDateTime.now();

	@Column(name = "TP_ORIGEM", nullable = false, length = 30)
	private String origem;

	@Column(name = "TP_ATOR", length = 30)
	private String ator;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_RESPONSAVEL")
	private Responsavel responsavel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_VETERINARIO")
	private Veterinario veterinario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_ANIMAL")
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CLINICA")
	private Clinica clinica;

	@Column(name = "DS_CANAL", length = 30)
	private String canal;

	@Lob
	@Column(name = "DS_CONTEXTO")
	private String contexto;

	@Lob
	@Column(name = "PAYLOAD_JSON")
	private String payloadJson;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTipoEvento() {
		return tipoEvento;
	}

	public void setTipoEvento(String tipoEvento) {
		this.tipoEvento = tipoEvento;
	}

	public LocalDateTime getDataEvento() {
		return dataEvento;
	}

	public void setDataEvento(LocalDateTime dataEvento) {
		this.dataEvento = dataEvento;
	}

	public String getOrigem() {
		return origem;
	}

	public void setOrigem(String origem) {
		this.origem = origem;
	}

	public String getAtor() {
		return ator;
	}

	public void setAtor(String ator) {
		this.ator = ator;
	}

	public Responsavel getResponsavel() {
		return responsavel;
	}

	public void setResponsavel(Responsavel responsavel) {
		this.responsavel = responsavel;
	}

	public Veterinario getVeterinario() {
		return veterinario;
	}

	public void setVeterinario(Veterinario veterinario) {
		this.veterinario = veterinario;
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

	public String getCanal() {
		return canal;
	}

	public void setCanal(String canal) {
		this.canal = canal;
	}

	public String getContexto() {
		return contexto;
	}

	public void setContexto(String contexto) {
		this.contexto = contexto;
	}

	public String getPayloadJson() {
		return payloadJson;
	}

	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}

}
