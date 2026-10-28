package br.com.fiap.arkive.service.notification;

import java.util.Map;

public record ExpoPushMessage(
		String to,
		String title,
		String body,
		Map<String, Object> data
) {
}

