package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SchemaAfterConfiguration")
class SchemaAfterConfigurationTest {

    private final SchemaAfterConfiguration configuration = new SchemaAfterConfiguration();

    @Test
    @DisplayName("Registra SchemaProperties no contexto")
    void enablesSchemaProperties() {
        assertThat(SchemaAfterConfiguration.class.getAnnotation(EnableConfigurationProperties.class).value())
                .containsExactly(SchemaProperties.class);
    }

    @Test
    @DisplayName("Populator after usa separador ;, UTF-8 e continueOnError false")
    void afterPopulatorUsesSemicolon() throws NoSuchMethodException {
        final var local = configuration.populator(SchemaPropertiesFixture.local().afterScripts());
        final var prod = configuration.populator(SchemaPropertiesFixture.prod().afterScripts());

        assertPopulator(local, ";",
                "util/columns.sql",
                "util/uniques.sql",
                "util/indexes.sql",
                "settlement/profile.sql",
                "settlement/checker.sql",
                "settlement/note.sql");
        assertPopulator(prod, ";", "util/columns.sql", "util/uniques.sql", "util/indexes.sql");
        final var method = SchemaAfterConfiguration.class.getDeclaredMethod("schemaAfter", DataSource.class,
                SchemaProperties.class);
        assertThat(method.getAnnotation(Bean.class).name()).containsExactly("schemaAfter");
        assertThat(method.getAnnotation(DependsOn.class).value()).containsExactly("entityManagerFactory");
    }

    @Test
    @DisplayName("Execução after envia name_purified, unicidade, índice e alice@example.com")
    void afterRunSendsColumnsUniquesIndexesAndAlice() throws Exception {
        final var statement = mock(Statement.class);
        when(statement.execute(anyString())).thenReturn(false);
        when(statement.getUpdateCount()).thenReturn(-1);
        when(statement.getMoreResults()).thenReturn(false);
        final var dataSource = mock(DataSource.class);
        final var connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        final var executed = ArgumentCaptor.forClass(String.class);

        configuration.schemaAfter(dataSource, SchemaPropertiesFixture.local()).afterPropertiesSet();

        verify(statement, atLeastOnce()).execute(executed.capture());
        final var sql = executed.getAllValues();
        final var name = firstIndex(sql, "name_purified");
        final var unique = firstIndex(sql, "profile_email_unique");
        final var index = firstIndex(sql, "profile_email_index");
        final var alice = firstIndex(sql, "alice@example.com");
        assertThat(name).isNotNegative();
        assertThat(name).isLessThan(unique);
        assertThat(unique).isLessThan(index);
        assertThat(index).isLessThan(alice);
    }

    private static void assertPopulator(final ResourceDatabasePopulator populator, final String separator,
            final String... paths) {
        assertThat(ReflectionTestUtils.getField(populator, "separator")).isEqualTo(separator);
        assertThat(ReflectionTestUtils.getField(populator, "sqlScriptEncoding"))
                .isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(ReflectionTestUtils.getField(populator, "continueOnError")).isEqualTo(false);
        assertThat(ReflectionTestUtils.getField(populator, "scripts"))
                .asInstanceOf(InstanceOfAssertFactories.list(ClassPathResource.class))
                .extracting(ClassPathResource::getPath)
                .containsExactly(paths);
    }

    private static int firstIndex(final List<String> statements, final String fragment) {
        for (var index = 0; index < statements.size(); index++) {
            if (statements.get(index).contains(fragment)) {
                return index;
            }
        }
        return -1;
    }

}
