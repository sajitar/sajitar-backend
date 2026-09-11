package com.sajitar.backend.domain.port;

import java.util.UUID;

public interface AccessTokenIssuer {

    TokenPair issue(UUID profileId);

}
