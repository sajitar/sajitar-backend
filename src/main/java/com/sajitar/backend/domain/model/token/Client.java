package com.sajitar.backend.domain.model.token;

public record Client(String name, String os, Device device) {

    public enum Device {

        DESKTOP("desktop"),
        MOBILE("mobile"),
        TABLET("tablet"),
        UNKNOWN("unknown");

        private final String value;

        Device(final String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static Device of(final String raw) {
            if (raw == null || raw.isBlank()) {
                return UNKNOWN;
            }
            for (final var device : values()) {
                if (device.value.equals(raw)) {
                    return device;
                }
            }
            return UNKNOWN;
        }

    }

}
