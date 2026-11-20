package com.sajitar.backend.adapter.out.persistence.term;

import java.io.Serializable;
import java.util.UUID;

import com.sajitar.backend.domain.model.term.Term;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TermId implements Serializable {

    private UUID id;

    private Term.Type type;

}
