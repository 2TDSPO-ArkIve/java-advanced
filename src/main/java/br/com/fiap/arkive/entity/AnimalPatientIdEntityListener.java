package br.com.fiap.arkive.entity;

import br.com.fiap.arkive.service.ArkivePatientIdGenerator;
import jakarta.persistence.PrePersist;

public class AnimalPatientIdEntityListener {

	private static final ArkivePatientIdGenerator GENERATOR = new ArkivePatientIdGenerator();

	@PrePersist
	public void assignPatientId(Animal animal) {
		if (animal.getArkivePatientId() == null) {
			animal.assignArkivePatientId(GENERATOR.generate());
		}
	}
}
