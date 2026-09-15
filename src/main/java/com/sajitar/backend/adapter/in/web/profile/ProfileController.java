package com.sajitar.backend.adapter.in.web.profile;

import static org.springframework.util.StringUtils.hasText;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contract.profile.CreateProfileRequest;
import com.sajitar.backend.adapter.in.web.contract.profile.PatchProfileRequest;
import com.sajitar.backend.adapter.in.web.contract.profile.ProfileApi;
import com.sajitar.backend.adapter.in.web.contract.profile.ProfileDetailsResponse;
import com.sajitar.backend.adapter.in.web.contract.profile.ProfilePageResponse;
import com.sajitar.backend.adapter.in.web.contract.profile.ProfileSummaryResponse;
import com.sajitar.backend.adapter.in.web.contract.profile.UpdateProfileRequest;
import com.sajitar.backend.application.command.profile.DeleteProfileCommand;
import com.sajitar.backend.application.query.profile.ListProfilesQuery;
import com.sajitar.backend.application.query.profile.ProfileCursor;
import com.sajitar.backend.application.usecase.profile.CreateProfileUseCase;
import com.sajitar.backend.application.usecase.profile.DeleteProfileUseCase;
import com.sajitar.backend.application.usecase.profile.GetProfileUseCase;
import com.sajitar.backend.application.usecase.profile.ListProfilesUseCase;
import com.sajitar.backend.application.usecase.profile.PatchProfileUseCase;
import com.sajitar.backend.application.usecase.profile.UpdateProfileUseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class ProfileController implements ProfileApi {

    private final CreateProfileUseCase createProfile;

    private final UpdateProfileUseCase updateProfile;

    private final PatchProfileUseCase patchProfile;

    private final DeleteProfileUseCase deleteProfile;

    private final GetProfileUseCase getProfile;

    private final ListProfilesUseCase listProfiles;

    @Override
    public ResponseEntity<ProfileSummaryResponse> postProfile(final CreateProfileRequest request) {
        return ResponseEntity.ok(ProfileSummaryResponse.from(createProfile.execute(request.toCommand())));
    }

    @Override
    public ResponseEntity<ProfileSummaryResponse> putProfile(final UUID id, final UpdateProfileRequest request) {
        return ResponseEntity.ok(ProfileSummaryResponse.from(updateProfile.execute(request.toCommand(id))));
    }

    @Override
    public ResponseEntity<ProfileSummaryResponse> patchProfile(final UUID id, final PatchProfileRequest request) {
        return ResponseEntity.ok(ProfileSummaryResponse.from(patchProfile.execute(request.toCommand(id))));
    }

    @Override
    public ResponseEntity<Void> deleteProfile(final UUID id) {
        deleteProfile.execute(new DeleteProfileCommand(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProfileSummaryResponse> getProfile(final UUID id) {
        return ResponseEntity.of(getProfile.execute(id).map(ProfileSummaryResponse::from));
    }

    @Override
    public ResponseEntity<ProfileDetailsResponse> getProfileDetails(final UUID id) {
        return ResponseEntity.of(getProfile.execute(id).map(ProfileDetailsResponse::from));
    }

    @Override
    public ResponseEntity<ProfilePageResponse> getProfiles(
            final String name,
            final String lastSeenName,
            final UUID lastSeenId,
            final int limit,
            final boolean reverse) {
        final var cursor = hasText(lastSeenName) && lastSeenId != null
                ? new ProfileCursor(lastSeenName, lastSeenId)
                : null;
        final var page = listProfiles.execute(new ListProfilesQuery(
                limit,
                reverse,
                hasText(name) ? name : null,
                cursor));
        return page.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(ProfilePageResponse.from(page));
    }

}
