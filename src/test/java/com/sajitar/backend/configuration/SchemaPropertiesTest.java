package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

@DisplayName("SchemaProperties")
class SchemaPropertiesTest {

    @Test
    @DisplayName("Tokeniza before e after, ignora vazio e prefixo classpath:")
    void tokenizesLocationsAndStripsClasspathPrefix() {
        final var properties = new SchemaProperties(
                "classpath:util/functions.sql, , util/enums.sql",
                "util/columns.sql,classpath:,classpath:util/uniques.sql");

        assertThat(properties.beforeScripts()).containsExactly("util/functions.sql", "util/enums.sql");
        assertThat(properties.afterScripts()).containsExactly("util/columns.sql", "util/uniques.sql");
    }

    @Test
    @DisplayName("Liga before e after pelo prefixo sajitar.schema")
    void bindsBeforeAndAfter() {
        final var source = new MapConfigurationPropertySource(Map.of(
                "sajitar.schema.before", SchemaPropertiesFixture.BEFORE,
                "sajitar.schema.after", SchemaPropertiesFixture.AFTER));

        final var properties = new Binder(source).bind("sajitar.schema", Bindable.of(SchemaProperties.class)).get();

        assertThat(properties.before()).isEqualTo(SchemaPropertiesFixture.BEFORE);
        assertThat(properties.after()).isEqualTo(SchemaPropertiesFixture.AFTER);
        assertThat(properties.beforeScripts()).containsExactly("util/functions.sql", "util/enums.sql");
        assertThat(properties.afterScripts()).containsExactly("util/columns.sql", "util/uniques.sql",
                "util/indexes.sql", "settlement/profile.sql", "settlement/checker.sql", "settlement/note.sql");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", ",,,", "classpath:" })
    @DisplayName("Rejeita before em branco")
    void rejectsBlankBefore(final String before) {
        final var thrown = catchThrowable(() -> new SchemaProperties(before, SchemaPropertiesFixture.AFTER));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", ",,,", "classpath:" })
    @DisplayName("Rejeita after em branco")
    void rejectsBlankAfter(final String after) {
        final var thrown = catchThrowable(() -> new SchemaProperties(SchemaPropertiesFixture.BEFORE, after));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after");
    }

}
