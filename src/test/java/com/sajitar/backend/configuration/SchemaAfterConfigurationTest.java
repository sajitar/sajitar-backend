package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptException;

@DisplayName("SchemaAfterConfiguration")
class SchemaAfterConfigurationTest {

    @Test
    @DisplayName("Depende do EntityManagerFactory")
    void dependsOnEntityManagerFactory() throws Exception {
        final var method = SchemaAfterConfiguration.class.getDeclaredMethod("schemaAfter", DataSource.class,
                SchemaProperties.class);
        assertThat(method.getAnnotation(DependsOn.class).value()).containsExactly("entityManagerFactory");
    }

    @Test
    @DisplayName("Populator aplica a lista after com separador ;")
    void populatorRunsAfterScripts() {
        final var populator = new SchemaAfterConfiguration()
                .schemaAfterPopulator(SchemaPropertiesFixture.defaults());

        assertThat(populator).extracting("separator").isEqualTo(";");
        assertThat(populator).extracting("continueOnError").isEqualTo(false);
        assertThat(populator).extracting("sqlScriptEncoding").isEqualTo("UTF-8");
        assertThat(populator).extracting("scripts").asList()
                .extracting(script -> ((Resource) script).getFilename())
                .containsExactly("columns.sql", "uniques.sql", "indexes.sql", "profile.sql", "checker.sql", "note.sql");
    }

    @Test
    @DisplayName("Lista PROD não inclui settlement")
    void prodListOmitsSettlement() {
        final var populator = new SchemaAfterConfiguration()
                .schemaAfterPopulator(SchemaPropertiesFixture.prod());

        assertThat(populator).extracting("scripts").asList()
                .extracting(script -> ((Resource) script).getFilename())
                .containsExactly("columns.sql", "uniques.sql", "indexes.sql");
    }

    @Test
    @DisplayName("Executa a lista after contra o DataSource")
    void executesScriptsAgainstDataSource() throws Exception {
        final var dataSource = mock(DataSource.class);
        final var connection = mock(Connection.class);
        final var statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(false);
        when(statement.getUpdateCount()).thenReturn(-1);
        when(statement.getMoreResults()).thenReturn(false);

        new SchemaAfterConfiguration()
                .schemaAfter(dataSource, SchemaPropertiesFixture.defaults())
                .afterPropertiesSet();

        final var sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        assertThat(sql.getAllValues())
                .anyMatch(statementSql -> statementSql.contains("name_purified"))
                .anyMatch(statementSql -> statementSql.contains("profile_email_unique"))
                .anyMatch(statementSql -> statementSql.contains("profile_email_index"))
                .anyMatch(statementSql -> statementSql.contains("alice@example.com"));
    }

    @Test
    @DisplayName("Falha de script derruba o boot")
    void scriptFailureStopsBoot() throws Exception {
        final var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new SchemaAfterConfiguration()
                .schemaAfter(dataSource, SchemaPropertiesFixture.defaults())
                .afterPropertiesSet())
                .isInstanceOf(ScriptException.class);
    }

}
