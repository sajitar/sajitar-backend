package com.sajitar.backend.adapter.in.web.controller.term;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contract.term.CreateTermRequest;
import com.sajitar.backend.adapter.in.web.contract.term.PatchTermRequest;
import com.sajitar.backend.adapter.in.web.contract.term.TermApi;
import com.sajitar.backend.adapter.in.web.contract.term.TermPageResponse;
import com.sajitar.backend.adapter.in.web.contract.term.TermResponse;
import com.sajitar.backend.application.query.term.ListTermsQuery;
import com.sajitar.backend.application.usecase.term.CreateTermTypeUseCase;
import com.sajitar.backend.application.usecase.term.CreateTermUseCase;
import com.sajitar.backend.application.usecase.term.DeleteTermUseCase;
import com.sajitar.backend.application.usecase.term.GetCurrentTermUseCase;
import com.sajitar.backend.application.usecase.term.GetTermUseCase;
import com.sajitar.backend.application.usecase.term.ListTermsUseCase;
import com.sajitar.backend.application.usecase.term.PatchTermUseCase;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.model.token.Session;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TermController implements TermApi {

    private final CreateTermUseCase createTerm;

    private final CreateTermTypeUseCase createTermType;

    private final PatchTermUseCase patchTerm;

    private final DeleteTermUseCase deleteTerm;

    private final GetTermUseCase getTerm;

    private final ListTermsUseCase listTerms;

    private final GetCurrentTermUseCase getCurrentTerm;

    @Override
    public ResponseEntity<TermResponse> postTerm(final Session session, final CreateTermRequest request) {
        return ResponseEntity.ok(TermResponse.line(createTerm.execute(request.toCommand(), session.profileId()), true));
    }

    @Override
    public ResponseEntity<TermResponse> postTermType(
            final UUID id, final Session session, final CreateTermRequest request) {
        return ResponseEntity.ok(
                TermResponse.line(createTermType.execute(request.toCommand(id), session.profileId()), true));
    }

    @Override
    public ResponseEntity<TermResponse> patchTerm(
            final UUID id, final String type, final Session session, final PatchTermRequest request) {
        final var parsed = Term.Type.parse(type);
        return ResponseEntity.ok(
                TermResponse.line(patchTerm.execute(request.toCommand(id, parsed), session.profileId()), true));
    }

    @Override
    public ResponseEntity<Void> deleteTerm(final UUID id, final Session session) {
        deleteTerm.execute(id, session.profileId());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TermResponse> getTerm(final UUID id, final String type, final Session session) {
        final var parsed = parseType(type);
        return ResponseEntity.ok(TermResponse.of(getTerm.execute(id, parsed, session.profileId()), parsed, true));
    }

    @Override
    public ResponseEntity<TermPageResponse> getTerms(
            final String type, final UUID lastSeenId, final int limit, final boolean reverse, final Session session) {
        final var parsed = parseType(type);
        final var page = listTerms.execute(new ListTermsQuery(parsed, lastSeenId, limit, reverse), session.profileId());
        return page.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(TermPageResponse.from(page, parsed));
    }

    @Override
    public ResponseEntity<TermResponse> getCurrentTerm(final String lang) {
        final var type = parseLang(lang);
        return ResponseEntity.ok(TermResponse.of(getCurrentTerm.execute(type), type, false));
    }

    private static Term.Type parseType(final String type) {
        return type == null ? null : Term.Type.parse(type);
    }

    private static Term.Type parseLang(final String lang) {
        return lang == null ? null : Term.Type.fromLang(lang);
    }

}
