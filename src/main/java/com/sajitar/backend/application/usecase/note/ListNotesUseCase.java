package com.sajitar.backend.application.usecase.note;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.application.query.note.ListNotesQuery;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NoteRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListNotesUseCase {

    private final NoteRepository notes;

    private final Validator validator;

    public Page<Note> execute(final ListNotesQuery query) {
        Constraints.requireValid(validator, query);
        final var content = notes.findPage(query.toCriteria());
        if (content.isEmpty()) {
            return Page.empty(query.reverse());
        }
        final var last = content.getLast();
        final long following = notes.countAfterCursor(
                query.toCriteria().withCursor(last.id(), query.reverse()));
        final long preceding = query.hasCursor()
                ? notes.countAfterCursor(
                        query.toCriteria().withCursor(content.getFirst().id(), !query.reverse()))
                : 0L;
        return new Page<>(content, preceding, following, query.reverse());
    }

}
