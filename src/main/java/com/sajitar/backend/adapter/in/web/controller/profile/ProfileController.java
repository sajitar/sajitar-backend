package com.sajitar.backend.adapter.in.web.controller.profile;

import static org.springframework.util.StringUtils.hasText;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contract.profile.ChangeOwnPasswordRequest;
import com.sajitar.backend.adapter.in.web.contract.profile.ConfirmPasswordRecoveryRequest;
import com.sajitar.backend.adapter.in.web.contract.profile.CreateProfileRequest;
import com.sajitar.backend.adapter.in.web.contract.profile.PatchProfileRequest;
import com.sajitar.backend.adapter.in.web.contract.profile.ProfileApi;
import com.sajitar.backend.adapter.in.web.contract.profile.ProfileDetailsResponse;
import com.sajitar.backend.adapter.in.web.contract.profile.ProfilePageResponse;
import com.sajitar.backend.adapter.in.web.contract.profile.ProfileSummaryResponse;
import com.sajitar.backend.adapter.in.web.contract.profile.RecoverPasswordRequest;
import com.sajitar.backend.adapter.in.web.contract.profile.UpdateProfileRequest;
import com.sajitar.backend.adapter.in.web.controller.token.RequestOrigins;
import com.sajitar.backend.application.command.profile.DeleteProfileCommand;
import com.sajitar.backend.application.query.profile.ListProfilesQuery;
import com.sajitar.backend.application.query.profile.ProfileCursor;
import com.sajitar.backend.application.usecase.profile.ChangeOwnPasswordUseCase;
import com.sajitar.backend.application.usecase.profile.ConfirmPasswordRecoveryUseCase;
import com.sajitar.backend.application.usecase.profile.CreateProfileUseCase;
import com.sajitar.backend.application.usecase.profile.DeleteProfileUseCase;
import com.sajitar.backend.application.usecase.profile.GetProfileUseCase;
import com.sajitar.backend.application.usecase.profile.ListProfilesUseCase;
import com.sajitar.backend.application.usecase.profile.PatchProfileUseCase;
import com.sajitar.backend.application.usecase.profile.RequestPasswordRecoveryUseCase;
import com.sajitar.backend.application.usecase.profile.UpdateProfileUseCase;
import com.sajitar.backend.domain.model.token.Session;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class ProfileController implements ProfileApi {

    private final CreateProfileUseCase createProfile;

    private final ChangeOwnPasswordUseCase changeOwnPassword;

    private final RequestPasswordRecoveryUseCase requestPasswordRecovery;

    private final ConfirmPasswordRecoveryUseCase confirmPasswordRecovery;

    private final UpdateProfileUseCase updateProfile;

    private final PatchProfileUseCase patchProfile;

    private final DeleteProfileUseCase deleteProfile;

    private final GetProfileUseCase getProfile;

    private final ListProfilesUseCase listProfiles;

    private final RequestOrigins origins;

    @Override
    public ResponseEntity<ProfileSummaryResponse> postProfile(final CreateProfileRequest request) {
        return ResponseEntity.ok(ProfileSummaryResponse.from(createProfile.execute(request.toCommand())));
    }

    @Override
    public ResponseEntity<Void> postPassword(
            final Session session,
            final ChangeOwnPasswordRequest request,
            final HttpServletRequest http) {
        changeOwnPassword.execute(request.toCommand(session.profileId(), origins.address(http)));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> postPasswordRecovery(
            final RecoverPasswordRequest request,
            final HttpServletRequest http) {
        requestPasswordRecovery.execute(request.toCommand(origins.address(http)));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> postPasswordConfirm(
            final ConfirmPasswordRecoveryRequest request,
            final HttpServletRequest http) {
        confirmPasswordRecovery.execute(request.toCommand(origins.address(http)));
        return ResponseEntity.noContent().build();
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
    public ResponseEntity<ProfileSummaryResponse> getProfile(final UUID id, final Session session) {
        return ResponseEntity.of(getProfile.execute(id, session.profileId()).map(ProfileSummaryResponse::from));
    }

    @Override
    public ResponseEntity<ProfileDetailsResponse> getProfileDetails(final UUID id, final Session session) {
        return ResponseEntity.of(getProfile.execute(id, session.profileId()).map(ProfileDetailsResponse::from));
    }

    @Override
    public ResponseEntity<ProfilePageResponse> getProfiles(
            final String name,
            final String lastSeenName,
            final UUID lastSeenId,
            final int limit,
            final boolean reverse,
            final Session session) {
        final var cursor = hasText(lastSeenName) && lastSeenId != null
                ? new ProfileCursor(lastSeenName, lastSeenId)
                : null;
        final var page = listProfiles.execute(new ListProfilesQuery(
                limit,
                reverse,
                hasText(name) ? name : null,
                cursor,
                session.profileId()));
        return page.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(ProfilePageResponse.from(page));
    }

}
