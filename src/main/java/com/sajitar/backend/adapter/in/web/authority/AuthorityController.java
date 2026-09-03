package com.sajitar.backend.adapter.in.web.authority;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contract.authority.AuthorityApi;
import com.sajitar.backend.adapter.in.web.contract.authority.AuthorityPageResponse;
import com.sajitar.backend.adapter.in.web.contract.authority.AuthorityResponse;
import com.sajitar.backend.adapter.in.web.contract.authority.CreateAuthorityRequest;
import com.sajitar.backend.adapter.in.web.contract.authority.PatchAuthorityRequest;
import com.sajitar.backend.adapter.in.web.contract.authority.UpdateAuthorityRequest;
import com.sajitar.backend.application.command.authority.DeleteAuthorityCommand;
import com.sajitar.backend.application.query.authority.GetAuthorityByProfileAndTypeQuery;
import com.sajitar.backend.application.query.authority.ListAuthoritiesQuery;
import com.sajitar.backend.application.usecase.authority.CreateAuthorityUseCase;
import com.sajitar.backend.application.usecase.authority.DeleteAuthorityUseCase;
import com.sajitar.backend.application.usecase.authority.GetAuthorityByProfileAndTypeUseCase;
import com.sajitar.backend.application.usecase.authority.GetAuthorityUseCase;
import com.sajitar.backend.application.usecase.authority.ListAuthoritiesUseCase;
import com.sajitar.backend.application.usecase.authority.PatchAuthorityUseCase;
import com.sajitar.backend.application.usecase.authority.UpdateAuthorityUseCase;
import com.sajitar.backend.domain.model.authority.Authority;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class AuthorityController implements AuthorityApi {

    private final CreateAuthorityUseCase createAuthority;

    private final UpdateAuthorityUseCase updateAuthority;

    private final PatchAuthorityUseCase patchAuthority;

    private final DeleteAuthorityUseCase deleteAuthority;

    private final GetAuthorityUseCase getAuthority;

    private final GetAuthorityByProfileAndTypeUseCase getAuthorityByProfileAndType;

    private final ListAuthoritiesUseCase listAuthorities;

    @Override
    public ResponseEntity<AuthorityResponse> postAuthority(final UUID profileId, final CreateAuthorityRequest request) {
        return ResponseEntity.ok(AuthorityResponse.from(createAuthority.execute(request.toCommand(profileId))));
    }

    @Override
    public ResponseEntity<AuthorityResponse> putAuthority(final UUID id, final UpdateAuthorityRequest request) {
        return ResponseEntity.ok(AuthorityResponse.from(updateAuthority.execute(request.toCommand(id))));
    }

    @Override
    public ResponseEntity<AuthorityResponse> patchAuthority(final UUID id, final PatchAuthorityRequest request) {
        return ResponseEntity.ok(AuthorityResponse.from(patchAuthority.execute(request.toCommand(id))));
    }

    @Override
    public ResponseEntity<Void> deleteAuthority(final UUID id) {
        deleteAuthority.execute(new DeleteAuthorityCommand(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AuthorityResponse> getAuthority(final UUID id) {
        return ResponseEntity.of(getAuthority.execute(id).map(AuthorityResponse::from));
    }

    @Override
    public ResponseEntity<?> getAuthorities(
            final UUID profileId,
            final String type,
            final String lastSeenType,
            final int limit,
            final boolean reverse) {
        if (type != null) {
            return ResponseEntity.of(getAuthorityByProfileAndType
                    .execute(new GetAuthorityByProfileAndTypeQuery(profileId, Authority.Type.parse(type)))
                    .map(AuthorityResponse::from));
        }
        final var query = new ListAuthoritiesQuery(
                profileId,
                limit,
                reverse,
                lastSeenType == null ? null : Authority.Type.parse(lastSeenType));
        final var page = listAuthorities.execute(query);
        return page.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(AuthorityPageResponse.from(page));
    }

}
