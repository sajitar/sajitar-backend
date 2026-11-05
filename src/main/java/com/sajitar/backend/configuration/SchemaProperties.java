package com.sajitar.backend.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "sajitar.schema")
public record SchemaProperties(String before, String after) {

    private static final String CLASSPATH_PREFIX = "classpath:";

    @ConstructorBinding
    public SchemaProperties {
        requireScripts("sajitar.schema.before", before);
        requireScripts("sajitar.schema.after", after);
    }

    List<String> beforeScripts() {
        return scripts(before);
    }

    List<String> afterScripts() {
        return scripts(after);
    }

    private static void requireScripts(final String name, final String csv) {
        if (!StringUtils.hasText(csv) || scripts(csv).isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static List<String> scripts(final String csv) {
        final var locations = new ArrayList<String>();
        for (final var token : StringUtils.tokenizeToStringArray(csv, ",", true, true)) {
            final var path = token.startsWith(CLASSPATH_PREFIX) ? token.substring(CLASSPATH_PREFIX.length()) : token;
            if (!path.isEmpty()) {
                locations.add(path);
            }
        }
        return List.copyOf(locations);
    }

}
