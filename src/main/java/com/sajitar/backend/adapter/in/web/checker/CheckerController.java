package com.sajitar.backend.adapter.in.web.checker;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contract.checker.CheckerApi;
import com.sajitar.backend.adapter.in.web.contract.checker.CheckerPageResponse;
import com.sajitar.backend.adapter.in.web.contract.checker.CheckerResponse;
import com.sajitar.backend.adapter.in.web.contract.checker.CreateCheckerRequest;
import com.sajitar.backend.adapter.in.web.contract.checker.PatchCheckerRequest;
import com.sajitar.backend.adapter.in.web.contract.checker.UpdateCheckerRequest;
import com.sajitar.backend.application.command.checker.DeleteCheckerCommand;
import com.sajitar.backend.application.query.checker.GetCheckerByProfileAndTypeQuery;
import com.sajitar.backend.application.query.checker.ListCheckersQuery;
import com.sajitar.backend.application.usecase.checker.CreateCheckerUseCase;
import com.sajitar.backend.application.usecase.checker.DeleteCheckerUseCase;
import com.sajitar.backend.application.usecase.checker.GetCheckerByProfileAndTypeUseCase;
import com.sajitar.backend.application.usecase.checker.GetCheckerUseCase;
import com.sajitar.backend.application.usecase.checker.ListCheckersUseCase;
import com.sajitar.backend.application.usecase.checker.PatchCheckerUseCase;
import com.sajitar.backend.application.usecase.checker.UpdateCheckerUseCase;
import com.sajitar.backend.domain.model.checker.Checker;

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
            final int limit,
            final boolean reverse) {
        if (type != null) {
            return ResponseEntity.of(getCheckerByProfileAndType
                    .execute(new GetCheckerByProfileAndTypeQuery(profileId, Checker.Type.parse(type)))
                    .map(CheckerResponse::from));
        }
        final var query = new ListCheckersQuery(
                profileId,
                limit,
                reverse,
                lastSeenType == null ? null : Checker.Type.parse(lastSeenType));
        final var page = listCheckers.execute(query);
        return page.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(CheckerPageResponse.from(page));
    }

}
