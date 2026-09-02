package com.sajitar.backend.controller;

import static org.springframework.util.StringUtils.hasText;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.controller.contract.CreateProfileRequest;
import com.sajitar.backend.controller.contract.ProfileApi;
import com.sajitar.backend.controller.contract.ProfileDetailsResponse;
import com.sajitar.backend.controller.contract.ProfilePageResponse;
import com.sajitar.backend.controller.contract.ProfileSummaryResponse;
import com.sajitar.backend.controller.contract.UpdateProfileRequest;
import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.service.ProfileService;
import com.sajitar.backend.util.Pagination;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class ProfileController implements ProfileApi {

    private final ProfileService service;

    private final PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<ProfileSummaryResponse> postProfile(final CreateProfileRequest request) {
        return ResponseEntity.ok(ProfileSummaryResponse.from(service.save(request.toProfile())));
    }

    @Override
    public ResponseEntity<ProfileSummaryResponse> putProfile(final UpdateProfileRequest request) {
        final var profile = request.toProfile();
        final var saved = hasText(request.password())
                ? service.save(profile, passwordEncoder)
                : service.save(profile);
        return ResponseEntity.ok(ProfileSummaryResponse.from(saved));
    }

    @Override
    public ResponseEntity<ProfileSummaryResponse> getProfile(final UUID id) {
        return ResponseEntity.of(service.findById(id).map(ProfileSummaryResponse::from));
    }

    @Override
    public ResponseEntity<ProfileDetailsResponse> getProfileDetails(final UUID id) {
        return ResponseEntity.of(service.findById(id).map(ProfileDetailsResponse::from));
    }

    @Override
    public ResponseEntity<ProfilePageResponse> getProfiles(
            final String name,
            final String lastSeenName,
            final UUID lastSeenId,
            final int limit,
            final boolean reverse) {
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
        return pagination.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(ProfilePageResponse.from(pagination));
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
        if (hasText(lastSeenName) && Objects.nonNull(lastSeenId)) {
            final var content = continuation.get();
            if (ObjectUtils.isNotEmpty(content)) {
                builder.content(content)
                        .followingElements(countAfter.apply(content.getLast(), reverse))
                        .precedingElements(countAfter.apply(content.getFirst(), !reverse));
            }
            return builder.build();
        }
        final var content = firstPage.get();
        if (ObjectUtils.isNotEmpty(content)) {
            builder.content(content).followingElements(countAfter.apply(content.getLast(), reverse));
        }
        return builder.build();
    }

    @FunctionalInterface
    private interface CountAfterCursor {

        default long apply(final Profile profile, final boolean reverse) {
            return apply(profile.getName(), profile.getId(), reverse);
        }

        long apply(String lastSeenName, UUID lastSeenId, boolean reverse);
    }

}
