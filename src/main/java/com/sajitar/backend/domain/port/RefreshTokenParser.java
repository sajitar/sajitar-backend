package com.sajitar.backend.domain.port;

import java.util.UUID;

public interface RefreshTokenParser {

    UUID profileId(String refreshToken);

}
