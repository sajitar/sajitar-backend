package com.sajitar.backend.domain.port;

import com.sajitar.backend.domain.model.mail.MailMessage;

public interface Mailer {

    void send(MailMessage message);

}
