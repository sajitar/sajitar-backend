package com.sajitar.backend.configuration;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MailPropertiesFixture {

    public static final String HOST = "127.0.0.1";

    public static final int PORT = 1025;

    public static final String FROM = "noreply@localhost";

    public static MailProperties defaults() {
        return new MailProperties(HOST, PORT, FROM);
    }

}
