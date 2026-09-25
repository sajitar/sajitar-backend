package com.sajitar.backend.application;

import java.time.Instant;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import com.sajitar.backend.domain.model.mail.MailMessage;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ChangePasswordMail {

    private static final String SUBJECT_KEY = "mail.change-password.subject";

    private static final String PREHEADER_KEY = "mail.change-password.preheader";

    private static final String HEADING_KEY = "mail.change-password.heading";

    private static final String BODY_KEY = "mail.change-password.body";

    private static final String HINT_KEY = "mail.change-password.hint";

    private static final String FOOTER_KEY = "mail.change-password.footer";

    public static MailMessage compose(
            final MessageSource messageSource,
            final Instant sentAt,
            final String to,
            final String code,
            final int changePasswordMaxAgeHours) {
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
                messageSource.getMessage(HINT_KEY, new Object[] { changePasswordMaxAgeHours }, locale),
                messageSource.getMessage(FOOTER_KEY, null, locale));
        return new MailMessage(to, subject, html);
    }

}
