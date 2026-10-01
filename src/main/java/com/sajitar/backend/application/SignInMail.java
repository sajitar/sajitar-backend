package com.sajitar.backend.application;

import java.time.Instant;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import com.sajitar.backend.domain.model.mail.MailMessage;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SignInMail {

    private static final String SUBJECT_KEY = "mail.sign-in.subject";

    private static final String PREHEADER_KEY = "mail.sign-in.preheader";

    private static final String HEADING_KEY = "mail.sign-in.heading";

    private static final String BODY_KEY = "mail.sign-in.body";

    private static final String HINT_KEY = "mail.sign-in.hint";

    private static final String FOOTER_KEY = "mail.sign-in.footer";

    public static MailMessage compose(
            final MessageSource messageSource,
            final Instant sentAt,
            final String to,
            final String code) {
        final var locale = LocaleContextHolder.getLocale();
        final var args = new Object[] { code };
        final var subject = messageSource.getMessage(
                SUBJECT_KEY,
                new Object[] { MailLayout.subjectWhen(sentAt) },
                locale);
        final var html = MailLayout.render(
                locale.getLanguage(),
                subject,
                messageSource.getMessage(PREHEADER_KEY, args, locale),
                messageSource.getMessage(HEADING_KEY, null, locale),
                messageSource.getMessage(BODY_KEY, args, locale),
                code,
                messageSource.getMessage(HINT_KEY, null, locale),
                messageSource.getMessage(FOOTER_KEY, null, locale));
        return new MailMessage(to, subject, html);
    }

}
