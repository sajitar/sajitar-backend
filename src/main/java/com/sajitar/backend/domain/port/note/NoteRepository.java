package com.sajitar.backend.domain.port.note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sajitar.backend.domain.model.note.Note;

public interface NoteRepository {

    Note save(Note note);

    Optional<Note> findById(UUID id);

    List<Note> findPage(NotePageCriteria criteria);

    long countAfterCursor(NotePageCriteria criteria);

    void deleteById(UUID id);

}
