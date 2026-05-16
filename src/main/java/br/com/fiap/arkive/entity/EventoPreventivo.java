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

import java.time.LocalDate;

@Entity
@Table(name = "TB_ARKIVE_EVENTO_PREVENTIVO")
public class EventoPreventivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_EVENTO_PREV")
	private Long id;

	@Column(name = "DT_APLICACAO")
	private LocalDate dataAplicacao;

	@Column(name = "DT_PROXIMO", nullable = false)
	private LocalDate dataProximo;

	@Column(name = "ST_STATUS", nullable = false, length = 20)
	private String status = "PENDENTE";

	@Column(name = "ST_ALERTA", nullable = false, length = 1)
	private String alerta = "N";

	@Lob
	@Column(name = "DS_OBSERVACAO")
	private String observacao;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_PROTOCOLO", nullable = false)
	private ProtocoloPreventivo protocolo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CONSULTA")
	private Consulta consulta;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getDataAplicacao() {
		return dataAplicacao;
	}

	public void setDataAplicacao(LocalDate dataAplicacao) {
		this.dataAplicacao = dataAplicacao;
	}

	public LocalDate getDataProximo() {
		return dataProximo;
	}

	public void setDataProximo(LocalDate dataProximo) {
		this.dataProximo = dataProximo;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAlerta() {
		return alerta;
	}

	public void setAlerta(String alerta) {
		this.alerta = alerta;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

	public Animal getAnimal() {
		return animal;
	}

	public void setAnimal(Animal animal) {
		this.animal = animal;
	}

	public ProtocoloPreventivo getProtocolo() {
		return protocolo;
	}

	public void setProtocolo(ProtocoloPreventivo protocolo) {
		this.protocolo = protocolo;
	}

	public Consulta getConsulta() {
		return consulta;
	}

	public void setConsulta(Consulta consulta) {
		this.consulta = consulta;
	}

}
