package br.com.fiap.arkive.service.notification;

import java.util.List;
import java.util.Map;

record ExpoPushResponse(List<ExpoPushTicket> data, List<ExpoPushApiError> errors) {
}

record ExpoPushTicket(String status, String id, String message, Map<String, Object> details) {
}

record ExpoPushApiError(String code, String message) {
}

