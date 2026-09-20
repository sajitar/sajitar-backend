package com.sajitar.backend.configuration;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AttemptPropertiesFixture {

    public static final int CREDENTIALS_MAX = 10;

    public static final int CREDENTIALS_WINDOW_SECONDS = 300;

    public static final int REFRESH_MAX = 30;

    public static final int REFRESH_WINDOW_SECONDS = 60;

    public static AttemptProperties defaults() {
        return new AttemptProperties(
                CREDENTIALS_MAX, CREDENTIALS_WINDOW_SECONDS, REFRESH_MAX, REFRESH_WINDOW_SECONDS, false);
    }

    public static AttemptProperties trustingForwardedFor() {
        return new AttemptProperties(
                CREDENTIALS_MAX, CREDENTIALS_WINDOW_SECONDS, REFRESH_MAX, REFRESH_WINDOW_SECONDS, true);
    }

    public static AttemptProperties tight() {
        return new AttemptProperties(2, 60, 2, 60, false);
    }

}
