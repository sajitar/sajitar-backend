package com.sajitar.backend.adapter.in.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contract.CheckerApi;
import com.sajitar.backend.adapter.in.web.contract.CheckerPageResponse;
import com.sajitar.backend.adapter.in.web.contract.CheckerResponse;
import com.sajitar.backend.adapter.in.web.contract.CreateCheckerRequest;
import com.sajitar.backend.adapter.in.web.contract.PatchCheckerRequest;
import com.sajitar.backend.adapter.in.web.contract.UpdateCheckerRequest;
import com.sajitar.backend.application.command.DeleteCheckerCommand;
import com.sajitar.backend.application.query.GetCheckerByProfileAndTypeQuery;
import com.sajitar.backend.application.query.ListCheckersQuery;
import com.sajitar.backend.application.usecase.CreateCheckerUseCase;
import com.sajitar.backend.application.usecase.DeleteCheckerUseCase;
import com.sajitar.backend.application.usecase.GetCheckerByProfileAndTypeUseCase;
import com.sajitar.backend.application.usecase.GetCheckerUseCase;
import com.sajitar.backend.application.usecase.ListCheckersUseCase;
import com.sajitar.backend.application.usecase.PatchCheckerUseCase;
import com.sajitar.backend.application.usecase.UpdateCheckerUseCase;
import com.sajitar.backend.domain.model.Checker;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class CheckerController implements CheckerApi {

    private final CreateCheckerUseCase createChecker;

    private final UpdateCheckerUseCase updateChecker;

    private final PatchCheckerUseCase patchChecker;

    private final DeleteCheckerUseCase deleteChecker;

    private final GetCheckerUseCase getChecker;

    private final GetCheckerByProfileAndTypeUseCase getCheckerByProfileAndType;

    private final ListCheckersUseCase listCheckers;

    @Override
    public ResponseEntity<CheckerResponse> postChecker(final UUID profileId, final CreateCheckerRequest request) {
        return ResponseEntity.ok(CheckerResponse.from(createChecker.execute(request.toCommand(profileId))));
    }

    @Override
    public ResponseEntity<CheckerResponse> putChecker(final UUID id, final UpdateCheckerRequest request) {
        return ResponseEntity.ok(CheckerResponse.from(updateChecker.execute(request.toCommand(id))));
    }

    @Override
    public ResponseEntity<CheckerResponse> patchChecker(final UUID id, final PatchCheckerRequest request) {
        return ResponseEntity.ok(CheckerResponse.from(patchChecker.execute(request.toCommand(id))));
    }

    @Override
    public ResponseEntity<Void> deleteChecker(final UUID id) {
        deleteChecker.execute(new DeleteCheckerCommand(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CheckerResponse> getChecker(final UUID id) {
        return ResponseEntity.of(getChecker.execute(id).map(CheckerResponse::from));
    }

    @Override
    public ResponseEntity<?> getCheckers(
            final UUID profileId,
            final String type,
            final String lastSeenType,
            final Integer limit) {
        if (type != null) {
            return ResponseEntity.of(getCheckerByProfileAndType
                    .execute(new GetCheckerByProfileAndTypeQuery(profileId, Checker.Type.parse(type)))
                    .map(CheckerResponse::from));
        }
        final var query = new ListCheckersQuery(
                profileId,
                limit,
                lastSeenType == null ? null : Checker.Type.parse(lastSeenType));
        final var content = listCheckers.execute(query);
        return content.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(CheckerPageResponse.from(query.limit(), query.lastSeenType(), content));
    }

}
