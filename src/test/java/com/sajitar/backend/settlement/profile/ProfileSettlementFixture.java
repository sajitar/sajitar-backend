package com.sajitar.backend.settlement.profile;

import java.util.UUID;

import lombok.experimental.UtilityClass;

/**
 * Registros fixos alinhados a {@code classpath:settlement/profile.sql} (ordem
 * de inserção; os IDs são estáveis e permitem asserções nos testes de API).
 */
@UtilityClass
public class ProfileSettlementFixture {

	/** Número de linhas {@code INSERT} em {@code settlement/profile.sql}. */
	public static final int SETTLEMENT_ROW_COUNT = 135;

	/** Primeiro registro inserido no script (Alice Alves). */
	public static final UUID ALICE_ID = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");
	public static final String ALICE_NAME = "Alice Alves";
	public static final String ALICE_DESCRIPTION = "Uma pessoa criativa e dedicada.";
	public static final String ALICE_BIRTHDAY = "1988-01-10";
	public static final String ALICE_EMAIL = "alice@example.com";

	public static final UUID BRUNO_ID = UUID.fromString("0198a0d3-bd61-7000-9b88-50cc3638e965");

	public static final UUID CARLA_ID = UUID.fromString("0198a5fa-1961-7000-e2fd-40ab328bc644");

	/** Substring de nome com uma ocorrência no script (ex.: “Henrique Silva”). */
	public static final String NAME_SEARCH_SILVA = "Silva";

	/** Substring de nome com múltiplas ocorrências (vários “… Queiroz” + “Ícaro Queiroz”). */
	public static final String NAME_SEARCH_QUEIROZ = "Queiroz";

	/** Substring que não ocorre em nenhum nome da massa settlement (busca vazia). */
	public static final String NAME_SEARCH_NO_MATCH = "zzz_sem_correspondencia_no_script_xyz";

	public static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

}
