package com.sajitar.backend.domain.port;

public interface PasswordHasher {

    String hash(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String hashedPassword);

}
