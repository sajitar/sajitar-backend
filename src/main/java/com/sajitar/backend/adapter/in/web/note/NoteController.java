package com.sajitar.backend.adapter.in.web.note;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contract.note.CreateNoteRequest;
import com.sajitar.backend.adapter.in.web.contract.note.NoteApi;
import com.sajitar.backend.adapter.in.web.contract.note.NotePageResponse;
import com.sajitar.backend.adapter.in.web.contract.note.NoteResponse;
import com.sajitar.backend.adapter.in.web.contract.note.PatchNoteRequest;
import com.sajitar.backend.adapter.in.web.contract.note.UpdateNoteRequest;
import com.sajitar.backend.application.command.note.DeleteNoteCommand;
import com.sajitar.backend.application.query.note.ListNotesQuery;
import com.sajitar.backend.application.usecase.note.CreateNoteUseCase;
import com.sajitar.backend.application.usecase.note.DeleteNoteUseCase;
import com.sajitar.backend.application.usecase.note.GetNoteUseCase;
import com.sajitar.backend.application.usecase.note.ListNotesUseCase;
import com.sajitar.backend.application.usecase.note.PatchNoteUseCase;
import com.sajitar.backend.application.usecase.note.UpdateNoteUseCase;
import com.sajitar.backend.domain.model.note.Note;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class NoteController implements NoteApi {

    private final CreateNoteUseCase createNote;

    private final UpdateNoteUseCase updateNote;

    private final PatchNoteUseCase patchNote;

    private final DeleteNoteUseCase deleteNote;

    private final GetNoteUseCase getNote;

    private final ListNotesUseCase listNotes;

    @Override
    public ResponseEntity<NoteResponse> postNote(final UUID profileId, final CreateNoteRequest request) {
        return ResponseEntity.ok(NoteResponse.from(createNote.execute(request.toCommand(profileId))));
    }

    @Override
    public ResponseEntity<NoteResponse> putNote(final UUID id, final UpdateNoteRequest request) {
        return ResponseEntity.ok(NoteResponse.from(updateNote.execute(request.toCommand(id))));
    }

    @Override
    public ResponseEntity<NoteResponse> patchNote(final UUID id, final PatchNoteRequest request) {
        return ResponseEntity.ok(NoteResponse.from(patchNote.execute(request.toCommand(id))));
    }

    @Override
    public ResponseEntity<Void> deleteNote(final UUID id) {
        deleteNote.execute(new DeleteNoteCommand(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<NoteResponse> getNote(final UUID id) {
        return ResponseEntity.of(getNote.execute(id).map(NoteResponse::from));
    }

    @Override
    public ResponseEntity<NotePageResponse> getNotes(
            final UUID profileId,
            final String type,
            final UUID lastSeenId,
            final int limit,
            final boolean reverse) {
        final var query = new ListNotesQuery(
                profileId,
                type == null ? null : Note.Type.parse(type),
                lastSeenId,
                limit,
                reverse);
        final var page = listNotes.execute(query);
        return page.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(NotePageResponse.from(page));
    }

}
