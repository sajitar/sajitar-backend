package com.sajitar.backend.application.usecase.term;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.term.TermRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCurrentTermUseCase {

    private final TermRepository terms;

    public List<Term> execute(final Term.Type lang) {
        final var currentId = terms.findCurrentId().orElseThrow(TermNotFoundException::new);
        final var lines = terms.findByIdAll(currentId);
        return lang == null ? lines : lines.stream().filter(line -> line.type() == lang).toList();
    }

}
