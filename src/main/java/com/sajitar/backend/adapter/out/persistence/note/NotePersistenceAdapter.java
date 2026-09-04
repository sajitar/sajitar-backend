package com.sajitar.backend.adapter.out.persistence.note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NotePageCriteria;
import com.sajitar.backend.domain.port.note.NoteRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class NotePersistenceAdapter implements NoteRepository {

    private final NoteJpaRepository jpa;

    @Override
    public Note save(final Note note) {
        return NotePersistenceMapper.toDomain(jpa.save(NotePersistenceMapper.toEntity(note)));
    }

    @Override
    public Optional<Note> findById(final UUID id) {
        return jpa.findById(id).map(NotePersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(final UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public List<Note> findPage(final NotePageCriteria criteria) {
        return findEntities(criteria).stream().map(NotePersistenceMapper::toDomain).toList();
    }

    private List<NoteJpaEntity> findEntities(final NotePageCriteria criteria) {
        if (criteria.hasTypeFilter()) {
            final var type = (short) criteria.type().value();
            if (criteria.reverse()) {
                return criteria.hasCursor()
                        ? jpa.findPageByProfileIdAndTypeDescendingAfter(
                                criteria.profileId(), type, criteria.lastSeenId(), criteria.limit())
                        : jpa.findPageByProfileIdAndTypeDescending(criteria.profileId(), type, criteria.limit());
            }
            return criteria.hasCursor()
                    ? jpa.findPageByProfileIdAndTypeAfter(
                            criteria.profileId(), type, criteria.lastSeenId(), criteria.limit())
                    : jpa.findPageByProfileIdAndType(criteria.profileId(), type, criteria.limit());
        }
        if (criteria.reverse()) {
            return criteria.hasCursor()
                    ? jpa.findPageByProfileIdDescendingAfter(
                            criteria.profileId(), criteria.lastSeenId(), criteria.limit())
                    : jpa.findPageByProfileIdDescending(criteria.profileId(), criteria.limit());
        }
        return criteria.hasCursor()
                ? jpa.findPageByProfileIdAfter(criteria.profileId(), criteria.lastSeenId(), criteria.limit())
                : jpa.findPageByProfileId(criteria.profileId(), criteria.limit());
    }

    @Override
    public long countAfterCursor(final NotePageCriteria criteria) {
        if (!criteria.hasCursor()) {
            return 0L;
        }
        if (criteria.hasTypeFilter()) {
            final var type = (short) criteria.type().value();
            return criteria.reverse()
                    ? jpa.countByProfileIdAndTypeAndIdBefore(criteria.profileId(), type, criteria.lastSeenId())
                    : jpa.countByProfileIdAndTypeAndIdAfter(criteria.profileId(), type, criteria.lastSeenId());
        }
        return criteria.reverse()
                ? jpa.countByProfileIdAndIdBefore(criteria.profileId(), criteria.lastSeenId())
                : jpa.countByProfileIdAndIdAfter(criteria.profileId(), criteria.lastSeenId());
    }

}
