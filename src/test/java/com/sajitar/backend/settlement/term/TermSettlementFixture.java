package com.sajitar.backend.settlement.term;

import java.util.UUID;

import lombok.experimental.UtilityClass;

/**
 * Registros fixos alinhados a {@code classpath:settlement/term.sql}.
 */
@UtilityClass
public class TermSettlementFixture {

    public static final UUID PREVIOUS_ID = UUID.fromString("019c4000-a111-7000-8000-111111111111");

    public static final UUID CURRENT_ID = UUID.fromString("019c4000-a222-7000-8000-222222222222");

}
