package com.sajitar.backend.adapter.in.web;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.sajitar.backend.domain.exception.AuthorityNotFoundException;
import com.sajitar.backend.domain.exception.AuthorityTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.exception.CheckerReplacesExhaustedException;
import com.sajitar.backend.domain.exception.CheckerTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.CheckerTypeRestrictedException;
import com.sajitar.backend.domain.exception.DomainException;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.InvalidAuthorityTypeException;
import com.sajitar.backend.domain.exception.InvalidCheckerTypeException;
import com.sajitar.backend.domain.exception.InvalidNoteTypeException;
import com.sajitar.backend.domain.exception.NoteNotFoundException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.exception.ProfileUnavailableException;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class WebExceptionHandler {

    static final String TYPE_MISMATCH_KEY = "validation.type.mismatch";

    private final MessageSource messageSource;

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, List<String>>> handle(final ConstraintViolationException exception) {
        final var allErrors = new HashMap<String, List<String>>();
        exception.getConstraintViolations().forEach(violation -> {
            final var propertyPath = violation.getPropertyPath().toString();
            final var propertyName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            allErrors.computeIfAbsent(propertyName, _ -> new LinkedList<>()).add(violation.getMessage());
        });
        return ResponseEntity.badRequest().body(allErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> handle(final MethodArgumentNotValidException exception) {
        final var allErrors = new HashMap<String, List<String>>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            allErrors.computeIfAbsent(error.getField(), _ -> new LinkedList<>())
                    .add(error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(allErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, List<String>>> handle(final MethodArgumentTypeMismatchException exception) {
        final var fullPropertyType = Optional.ofNullable(exception.getRequiredType()).map(Class::getName).orElse("unknown");
        final var propertyType = fullPropertyType.substring(fullPropertyType.lastIndexOf('.') + 1);
        final var propertyName = exception.getName();
        return ResponseEntity.badRequest()
                .body(Map.of(propertyName, List.of(translate(TYPE_MISMATCH_KEY, propertyType))));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, List<String>>> handle(final DomainException exception) {
        return switch (exception) {
            case EmailAlreadyRegisteredException conflict -> ResponseEntity.status(CONFLICT)
                    .body(translateAll(conflict.content()));
            case CheckerTypeAlreadyExistsException conflict -> ResponseEntity.status(CONFLICT)
                    .body(translateAll(conflict.content()));
            case AuthorityTypeAlreadyExistsException conflict -> ResponseEntity.status(CONFLICT)
                    .body(translateAll(conflict.content()));
            case CheckerTypeRestrictedException forbidden -> ResponseEntity.status(FORBIDDEN)
                    .body(translateAll(forbidden.content()));
            case InvalidCheckerTypeException invalid -> ResponseEntity.badRequest()
                    .body(Map.of("type", List.of(translate(InvalidCheckerTypeException.MESSAGE_KEY, invalid.rejectedValue()))));
            case InvalidAuthorityTypeException invalid -> ResponseEntity.badRequest()
                    .body(Map.of("type", List.of(translate(InvalidAuthorityTypeException.MESSAGE_KEY, invalid.rejectedValue()))));
            case InvalidNoteTypeException invalid -> ResponseEntity.badRequest()
                    .body(Map.of("type", List.of(translate(InvalidNoteTypeException.MESSAGE_KEY, invalid.rejectedValue()))));
            case CheckerReplacesExhaustedException exhausted -> ResponseEntity.badRequest()
                    .body(translateAll(exhausted.content()));
            case ProfileUnavailableException unavailable -> ResponseEntity.status(NOT_FOUND)
                    .body(translateAll(unavailable.content()));
            case ProfileNotFoundException _ -> ResponseEntity.notFound().build();
            case CheckerNotFoundException _ -> ResponseEntity.notFound().build();
            case AuthorityNotFoundException _ -> ResponseEntity.notFound().build();
            case NoteNotFoundException _ -> ResponseEntity.notFound().build();
        };
    }

    private Map<String, List<String>> translateAll(final Map<String, List<String>> content) {
        final var body = new HashMap<String, List<String>>();
        content.forEach((field, keys) -> body.put(field, keys.stream().map(this::translate).toList()));
        return body;
    }

    private String translate(final String key, final Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

}
