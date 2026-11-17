package com.sajitar.backend.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sajitar.schema")
public record SchemaProperties(String before, String after) {

    private static final String CLASSPATH_PREFIX = "classpath:";

    public SchemaProperties {
        if (scripts(before).isEmpty()) {
            throw new IllegalArgumentException("sajitar.schema.before must not be blank");
        }
        if (scripts(after).isEmpty()) {
            throw new IllegalArgumentException("sajitar.schema.after must not be blank");
        }
    }

    public List<String> beforeScripts() {
        return scripts(before);
    }

    public List<String> afterScripts() {
        return scripts(after);
    }

    private static List<String> scripts(final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        final var paths = new ArrayList<String>();
        for (final var item : value.split(",", -1)) {
            var path = item.strip();
            if (path.startsWith(CLASSPATH_PREFIX)) {
                path = path.substring(CLASSPATH_PREFIX.length()).strip();
            }
            if (!path.isEmpty()) {
                paths.add(path);
            }
        }
        return List.copyOf(paths);
    }

}
