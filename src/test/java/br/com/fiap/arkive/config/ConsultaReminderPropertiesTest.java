package br.com.fiap.arkive.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultaReminderPropertiesTest {

	@Test
	void padraoEhTrintaMinutosELimitesSaoValidados() {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			var validator = factory.getValidator();
			ConsultaReminderProperties properties = new ConsultaReminderProperties();
			assertEquals(30, properties.getMinutesBefore());
			assertEquals(0, validator.validate(properties).size());

			properties.setMinutesBefore(0);
			assertEquals(1, validator.validate(properties).size());
			properties.setMinutesBefore(1441);
			assertEquals(1, validator.validate(properties).size());
		}
	}
}
