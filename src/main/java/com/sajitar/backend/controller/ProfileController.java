package com.sajitar.backend.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.util.StringUtils.hasText;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.service.ProfileService;
import com.sajitar.backend.util.Pagination;
import com.sajitar.backend.util.Routes;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = Routes.PROFILE, produces = { APPLICATION_JSON_VALUE })
public class ProfileController {
    private final ProfileService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable final UUID id) {
        return ResponseEntity.of(service.findById(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> getProfileDetails(@PathVariable final UUID id) {
        return ResponseEntity.of(service.findById(id).map(ProfileDetails::from));
    }

    @GetMapping
    public ResponseEntity<?> getProfiles(
            @RequestParam(required = false) final String name,
            @RequestParam(required = false) final String lastSeenName,
            @RequestParam(required = false) final UUID lastSeenId,
            @RequestParam(defaultValue = "100", required = false) final int limit,
            @RequestParam(defaultValue = "false", required = false) final boolean reverse) {
        final var pagination = hasText(name)
                ? paginate(limit, lastSeenName, lastSeenId, reverse,
                        () -> service.findByNameContainingIgnoreCase(limit, name, reverse),
                        () -> service.findByNameContainingIgnoreCase(limit, lastSeenName, lastSeenId, name, reverse),
                        (lastName, lastId, direction) -> {
                            return service.countByNameContainingIgnoreCase(lastName, lastId, name, direction);
                        })
                : paginate(limit, lastSeenName, lastSeenId, reverse,
                        () -> service.findAll(limit, reverse),
                        () -> service.findAll(limit, lastSeenName, lastSeenId, reverse),
                        service::countAll);
        return pagination.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(pagination);
    }

    private static Pagination<Profile> paginate(
            final int limit,
            final String lastSeenName,
            final UUID lastSeenId,
            final boolean reverse,
            final Supplier<List<Profile>> firstPage,
            final Supplier<List<Profile>> continuation,
            final CountAfterCursor countAfter) {
        final var builder = Pagination.<Profile>builder().reverse(reverse);
        if (!(hasText(lastSeenName) || Objects.nonNull(lastSeenId))) {
            final var content = firstPage.get();
            builder.content(content);
            Optional.ofNullable(content).filter(ObjectUtils::isNotEmpty).map(List::getLast).ifPresent(profile -> {
                builder.followingElements(countAfter.apply(profile.getName(), profile.getId(), reverse));
            });
            return builder.build();
        }
        final var content = continuation.get();
        builder.content(content);
        Optional.ofNullable(content).filter(ObjectUtils::isNotEmpty).map(List::getLast).ifPresent(profile -> {
            builder.followingElements(countAfter.apply(profile.getName(), profile.getId(), reverse));
        });
        Optional.ofNullable(content).filter(ObjectUtils::isNotEmpty).map(List::getFirst).ifPresent(profile -> {
            builder.precedingElements(countAfter.apply(profile.getName(), profile.getId(), !reverse));
        });
        return builder.build();
    }

    @FunctionalInterface
    private interface CountAfterCursor {
        long apply(String lastSeenName, UUID lastSeenId, boolean reverse);
    }

    private static record ProfileDetails(UUID id, String name, String description, LocalDate birthday, String email) {

        static ProfileDetails from(final Profile profile) {
            return new ProfileDetails(
                    profile.getId(),
                    profile.getName(),
                    profile.getDescription(),
                    profile.getBirthday(),
                    profile.getEmail());
        }

    }

}
