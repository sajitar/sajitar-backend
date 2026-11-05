package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
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
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptException;

@DisplayName("SchemaBeforeConfiguration")
class SchemaBeforeConfigurationTest {

    @Test
    @DisplayName("O EntityManagerFactory espera o bean schemaBefore")
    void entityManagerFactoryDependsOnSchemaBefore() {
        assertThat(SchemaBeforeConfiguration.entityManagerFactoryDependsOnSchemaBefore())
                .isInstanceOf(EntityManagerFactoryDependsOnPostProcessor.class);
    }

    @Test
    @DisplayName("Populator aplica a lista before com separador ;;")
    void populatorRunsBeforeScripts() {
        final var populator = new SchemaBeforeConfiguration()
                .schemaBeforePopulator(SchemaPropertiesFixture.defaults());

        assertThat(populator).extracting("separator").isEqualTo(";;");
        assertThat(populator).extracting("continueOnError").isEqualTo(false);
        assertThat(populator).extracting("sqlScriptEncoding").isEqualTo("UTF-8");
        assertThat(populator).extracting("scripts").asInstanceOf(LIST)
                .extracting(script -> ((Resource) script).getFilename())
                .containsExactly("functions.sql", "enums.sql");
    }

    @Test
    @DisplayName("Executa a lista before contra o DataSource")
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

        new SchemaBeforeConfiguration()
                .schemaBefore(dataSource, SchemaPropertiesFixture.defaults())
                .afterPropertiesSet();

        final var sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        assertThat(sql.getAllValues())
                .anyMatch(statementSql -> statementSql.contains("purify"))
                .anyMatch(statementSql -> statementSql.contains("profile_type"));
    }

    @Test
    @DisplayName("Falha de script derruba o boot")
    void scriptFailureStopsBoot() throws Exception {
        final var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new SchemaBeforeConfiguration()
                .schemaBefore(dataSource, SchemaPropertiesFixture.defaults())
                .afterPropertiesSet())
                .isInstanceOf(ScriptException.class);
    }

}
