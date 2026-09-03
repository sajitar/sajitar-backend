package com.sajitar.backend.settlement;

import java.util.UUID;

/**
 * Registros fixos alinhados a {@code classpath:settlement/checker.sql}.
 */
public final class CheckerSettlementFixture {

    public static final UUID ALICE_CHANGE_EMAIL_ID = UUID.fromString("019c1000-a111-7000-8000-111111111111");

    public static final UUID ALICE_VERIFY_EMAIL_ID = UUID.fromString("019c1000-a112-7000-8000-222222222222");

    public static final UUID ALICE_CHANGE_PASSWORD_ID = UUID.fromString("019c1000-a113-7000-8000-333333333333");

    public static final UUID BRUNO_CHANGE_EMAIL_ID = UUID.fromString("019c1000-b111-7000-8000-444444444444");

    public static final UUID CARLA_ID = UUID.fromString("0198a5fa-1961-7000-e2fd-40ab328bc644");

    public static final UUID BRUNO_ID = UUID.fromString("0198a0d3-bd61-7000-9b88-50cc3638e965");

    private CheckerSettlementFixture() {
    }

}
