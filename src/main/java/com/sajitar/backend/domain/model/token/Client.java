package com.sajitar.backend.domain.model.token;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

public record Client(String name, String os, Device device) {

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public enum Device {

        DESKTOP("desktop"),
        MOBILE("mobile"),
        TABLET("tablet"),
        UNKNOWN("unknown");

        private final String value;

        public static Device of(final String raw) {
            if (raw == null || raw.isBlank()) {
                return UNKNOWN;
            }
            for (final var device : values()) {
                if (device.value().equals(raw)) {
                    return device;
                }
            }
            return UNKNOWN;
        }

    }

}
