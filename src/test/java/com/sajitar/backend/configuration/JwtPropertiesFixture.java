package com.sajitar.backend.configuration;

public final class JwtPropertiesFixture {

    public static final String SECRET = "01234567890123456789012345678901";

    public static final int EXPIRATION_SECONDS = 3600;

    public static final int REFRESH_EXPIRATION_SECONDS = 604800;

    public static final int SESSION_MAX_SECONDS = 2592000;

    public static final int MAX_SESSIONS_PER_PROFILE = 10;

    public static final int REFRESH_GRACE_SECONDS = 15;

    public static final String ISSUER = "sajitar-backend";

    public static final String AUDIENCE = "sajitar-app";

    private JwtPropertiesFixture() {
    }

    public static JwtProperties defaults() {
        return new JwtProperties(SECRET, EXPIRATION_SECONDS, REFRESH_EXPIRATION_SECONDS, SESSION_MAX_SECONDS,
                MAX_SESSIONS_PER_PROFILE, REFRESH_GRACE_SECONDS, ISSUER, AUDIENCE);
    }

    public static JwtProperties with(final int sessionMaxSeconds, final int maxSessionsPerProfile,
            final int refreshGraceSeconds) {
        return new JwtProperties(SECRET, EXPIRATION_SECONDS, REFRESH_EXPIRATION_SECONDS, sessionMaxSeconds,
                maxSessionsPerProfile, refreshGraceSeconds, ISSUER, AUDIENCE);
    }

}
