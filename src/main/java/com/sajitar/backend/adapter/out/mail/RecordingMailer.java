package com.sajitar.backend.adapter.out.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sajitar.backend.domain.model.mail.MailMessage;
import com.sajitar.backend.domain.port.Mailer;

@Component
@Profile("test")
public class RecordingMailer implements Mailer {

    private final List<MailMessage> sent = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void send(final MailMessage message) {
        sent.add(message);
    }

    public List<MailMessage> sent() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }

}
