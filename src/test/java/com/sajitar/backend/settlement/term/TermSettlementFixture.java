package com.sajitar.backend.settlement.term;

import java.util.UUID;

import lombok.experimental.UtilityClass;

/**
 * Registros fixos alinhados a {@code classpath:settlement/term.sql}.
 */
@UtilityClass
public class TermSettlementFixture {

    /** id vigente: EN, PT e ES publicados (draft false). */
    public static final UUID PUBLISHED_ID = UUID.fromString("019c6000-a111-7000-8000-aaaaaaaaaaaa");

    /** id rascunho completo: EN, PT e ES em draft true. */
    public static final UUID DRAFT_ALL_ID = UUID.fromString("019c6000-b222-7000-8000-bbbbbbbbbbbb");

    /** id rascunho incompleto: só EN em draft true. */
    public static final UUID DRAFT_EN_ID = UUID.fromString("019c6000-c333-7000-8000-cccccccccccc");

    public static final UUID UNKNOWN_ID = UUID.fromString("019c6000-f999-7000-8000-ffffffffffff");

}
