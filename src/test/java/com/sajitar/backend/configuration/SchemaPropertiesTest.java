package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("SchemaProperties")
class SchemaPropertiesTest {

    @Test
    @DisplayName("O binder lê sajitar.schema.before e sajitar.schema.after")
    void binderReadsBeforeAndAfter() {
        final var environment = new MockEnvironment()
                .withProperty("sajitar.schema.before", "classpath:util/functions.sql, , util/enums.sql")
                .withProperty("sajitar.schema.after", "util/columns.sql,classpath:,classpath:util/uniques.sql");

        final var properties = Binder.get(environment)
                .bind("sajitar.schema", Bindable.of(SchemaProperties.class))
                .get();

        assertThat(SchemaProperties.class.getAnnotation(ConfigurationProperties.class).prefix())
                .isEqualTo("sajitar.schema");
        assertThat(properties.before()).isEqualTo("classpath:util/functions.sql, , util/enums.sql");
        assertThat(properties.after()).isEqualTo("util/columns.sql,classpath:,classpath:util/uniques.sql");
        assertThat(properties.beforeScripts()).containsExactly("util/functions.sql", "util/enums.sql");
        assertThat(properties.afterScripts()).containsExactly("util/columns.sql", "util/uniques.sql");
    }

    @Test
    @DisplayName("Remove classpath: e ignora item vazio")
    void removesClasspathPrefixAndIgnoresEmptyItems() {
        final var properties = new SchemaProperties(
                " classpath:  util/functions.sql , , util/enums.sql ",
                " util/columns.sql , classpath: , classpath:util/uniques.sql ");

        assertThat(properties.beforeScripts()).containsExactly("util/functions.sql", "util/enums.sql");
        assertThat(properties.afterScripts()).containsExactly("util/columns.sql", "util/uniques.sql");
    }

    @Test
    @DisplayName("Lista after local segue columns, uniques, indexes, profile, checker e note")
    void localAfterScriptsKeepSettlementOrder() {
        final var properties = SchemaPropertiesFixture.local();

        assertThat(properties.before()).isEqualTo(SchemaPropertiesFixture.BEFORE);
        assertThat(properties.after()).isEqualTo(SchemaPropertiesFixture.AFTER_LOCAL);
        assertThat(properties.beforeScripts()).containsExactly("util/functions.sql", "util/enums.sql");
        assertThat(properties.afterScripts()).containsExactly(
                "util/columns.sql",
                "util/uniques.sql",
                "util/indexes.sql",
                "settlement/profile.sql",
                "settlement/checker.sql",
                "settlement/note.sql");
    }

    @Test
    @DisplayName("Lista after de produção fica só em columns, uniques e indexes")
    void prodAfterScriptsOmitSettlement() {
        final var properties = SchemaPropertiesFixture.prod();

        assertThat(properties.beforeScripts()).containsExactly("util/functions.sql", "util/enums.sql");
        assertThat(properties.afterScripts()).containsExactly(
                "util/columns.sql",
                "util/uniques.sql",
                "util/indexes.sql");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", ",,,", "classpath:", "classpath: , , classpath:" })
    @DisplayName("Lista before sem caminho interrompe a subida")
    void rejectsBlankBefore(final String before) {
        final var thrown = catchThrowable(() -> new SchemaProperties(before, SchemaPropertiesFixture.AFTER_LOCAL));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sajitar.schema.before must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", ",,,", "classpath:", "classpath: , , classpath:" })
    @DisplayName("Lista after sem caminho interrompe a subida")
    void rejectsBlankAfter(final String after) {
        final var thrown = catchThrowable(() -> new SchemaProperties(SchemaPropertiesFixture.BEFORE, after));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sajitar.schema.after must not be blank");
    }

}
