package com.sajitar.backend.service;

import static org.springframework.http.HttpStatus.CONFLICT;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.repository.ProfileRepository;
import com.sajitar.backend.util.ResourceException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository repository;


    private record Validator(Profile profile, ResourceException resourceException) {

        public Validator(final Profile profile) {
            this(profile, ResourceException.builder().status(CONFLICT).build());
        }

        public void validate(final Optional<Profile> result, final String fieldName) {
            result.ifPresent(content -> {
                if (!content.getId().equals(profile.getId())) {
                    resourceException.getContent().computeIfAbsent(fieldName, key -> {
                        return new LinkedList<>();
                    }).add("deve ser um e-mail não registrado");
                }
            });
        }
    }

    public Profile save(@Valid final Profile profile) {
        final var validator = new Validator(profile);
        validator.validate(repository.findByEmail(profile.getEmail()), "email");
        if (!validator.resourceException.getContent().isEmpty()) {
            throw validator.resourceException;
        } else {
            return repository.save(profile);
        }
    }

    public Profile save(@Valid final Profile profile, final PasswordEncoder passwordEncoder) {
        final var validator = new Validator(profile);
        validator.validate(repository.findByEmail(profile.getEmail()), "email");
        if (!validator.resourceException.getContent().isEmpty()) {
            throw validator.resourceException;
        } else {
            return repository.save(profile.withPassword(passwordEncoder.encode(profile.getPassword())));
        }
    }

    public Optional<Profile> findById(@NotNull final UUID id) {
        return repository.findById(id);
    }

    public Optional<Profile> findByEmail(final @Email String email) {
        return repository.findByEmail(email);
    }

    public List<Profile> findAll(final @Limit Integer limit, final @NotNull Boolean reverse) {
        return reverse ? repository.findAllDescending(limit) : repository.findAllAscending(limit);
    }

    public List<Profile> findAll(
            final @Limit Integer limit,
            final @Name String lastSeenName,
            final @NotNull UUID lastSeenId,
            final @NotNull Boolean reverse) {
        return reverse
                ? repository.findAllDescendingAfter(limit, lastSeenName, lastSeenId)
                : repository.findAllAscendingAfter(limit, lastSeenName, lastSeenId);
    }

    public List<Profile> findByNameContainingIgnoreCase(
            final @Limit Integer limit,
            final @NotBlank String name,
            final @NotNull Boolean reverse) {
        return reverse
                ? repository.findByNameContainingIgnoreCaseDescending(limit, name)
                : repository.findByNameContainingIgnoreCaseAscending(limit, name);
    }

    public List<Profile> findByNameContainingIgnoreCase(
            final @Limit Integer limit,
            final @Name String lastSeenName,
            final @NotNull UUID lastSeenId,
            final @NotBlank String name,
            final @NotNull Boolean reverse) {
        return reverse
                ? repository.findByNameContainingIgnoreCaseDescendingAfter(limit, lastSeenName, lastSeenId, name)
                : repository.findByNameContainingIgnoreCaseAscendingAfter(limit, lastSeenName, lastSeenId, name);
    }

    public long countAll() {
        return repository.countForFindAll();
    }

    public long countAll(
            final @Name String lastSeenName,
            final @NotNull UUID lastSeenId,
            final @NotNull Boolean reverse) {
        return reverse
                ? repository.countForFindAllDescendingAfter(lastSeenName, lastSeenId)
                : repository.countForFindAllAscendingAfter(lastSeenName, lastSeenId);
    }

    public long countByNameContainingIgnoreCase(final @NotBlank String name) {
        return repository.countForFindByNameContainingIgnoreCase(name);
    }

    public long countByNameContainingIgnoreCase(
            final @Name String lastSeenName,
            final @NotNull UUID lastSeenId,
            final @NotBlank String name,
            final @NotNull Boolean reverse) {
        return reverse
                ? repository.countForFindByNameContainingIgnoreCaseDescendingAfter(lastSeenName, lastSeenId, name)
                : repository.countForFindByNameContainingIgnoreCaseAscendingAfter(lastSeenName, lastSeenId, name);
    }

}
