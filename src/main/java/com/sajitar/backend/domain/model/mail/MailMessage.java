package com.sajitar.backend.domain.model.mail;

public record MailMessage(String to, String subject, String body) {

    public MailMessage {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("to must not be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body must not be blank");
        }
    }

}
