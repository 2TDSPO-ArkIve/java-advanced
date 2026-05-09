package br.com.fiap.arkive.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HealthService {

	public Map<String, String> getHealth() {
		return Map.of(
				"status", "UP",
				"application", "Arkive API"
		);
	}

}
