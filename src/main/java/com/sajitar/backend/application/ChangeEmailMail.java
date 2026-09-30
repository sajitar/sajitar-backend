package com.sajitar.backend.application;

import java.time.Instant;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import com.sajitar.backend.domain.model.mail.MailMessage;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ChangeEmailMail {

    private static final String RECOVERY_PREFIX = "mail.change-email.recovery";

    private static final String CONFIRM_PREFIX = "mail.change-email.confirm";

    public static MailMessage composeRecovery(
            final MessageSource messageSource,
            final Instant sentAt,
            final String to,
            final String code,
            final int changeEmailMaxAgeMinutes) {
        return compose(RECOVERY_PREFIX, messageSource, sentAt, to, code, changeEmailMaxAgeMinutes);
    }

    public static MailMessage composeConfirm(
            final MessageSource messageSource,
            final Instant sentAt,
            final String to,
            final String code,
            final int changeEmailMaxAgeMinutes) {
        return compose(CONFIRM_PREFIX, messageSource, sentAt, to, code, changeEmailMaxAgeMinutes);
    }

    private static MailMessage compose(
            final String prefix,
            final MessageSource messageSource,
            final Instant sentAt,
            final String to,
            final String code,
            final int changeEmailMaxAgeMinutes) {
        final var locale = LocaleContextHolder.getLocale();
        final var args = new Object[] { code };
        final var subject = messageSource.getMessage(
                prefix + ".subject",
                new Object[] { MailLayout.subjectWhen(sentAt) },
                locale);
        final var html = MailLayout.render(
                locale.getLanguage(),
                subject,
                messageSource.getMessage(prefix + ".preheader", args, locale),
                messageSource.getMessage(prefix + ".heading", null, locale),
                messageSource.getMessage(prefix + ".body", args, locale),
                code,
                messageSource.getMessage(prefix + ".hint", new Object[] { changeEmailMaxAgeMinutes }, locale),
                messageSource.getMessage(prefix + ".footer", null, locale));
        return new MailMessage(to, subject, html);
    }

}
