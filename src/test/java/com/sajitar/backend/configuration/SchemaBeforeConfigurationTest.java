package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SchemaBeforeConfiguration")
class SchemaBeforeConfigurationTest {

    private final SchemaBeforeConfiguration configuration = new SchemaBeforeConfiguration();

    @Test
    @DisplayName("Registra SchemaProperties no contexto")
    void enablesSchemaProperties() {
        assertThat(SchemaBeforeConfiguration.class.getAnnotation(EnableConfigurationProperties.class).value())
                .containsExactly(SchemaProperties.class);
    }

    @Test
    @DisplayName("Populator before usa separador ;;, UTF-8 e continueOnError false")
    void beforePopulatorUsesDoubleSemicolon() throws NoSuchMethodException {
        final var populator = configuration.populator(SchemaPropertiesFixture.local().beforeScripts());

        assertPopulator(populator, ";;", "util/functions.sql", "util/enums.sql");
        final var method = SchemaBeforeConfiguration.class.getDeclaredMethod("schemaBefore", DataSource.class,
                SchemaProperties.class);
        assertThat(method.getAnnotation(Bean.class).name()).containsExactly("schemaBefore");
    }

    @Test
    @DisplayName("EntityManagerFactory depende do bean schemaBefore")
    void entityManagerFactoryDependsOnSchemaBefore() {
        final var factory = new DefaultListableBeanFactory();
        final var definition = new RootBeanDefinition(LocalContainerEntityManagerFactoryBean.class);
        factory.registerBeanDefinition("entityManagerFactory", definition);

        configuration.entityManagerFactoryDependsOnPostProcessor().postProcessBeanFactory(factory);

        assertThat(definition.getDependsOn()).contains("schemaBefore");
    }

    @Test
    @DisplayName("Execução before envia SQL com purify e depois profile_type")
    void beforeRunSendsPurifyAndProfileType() throws Exception {
        final var statement = statement();
        final var executed = ArgumentCaptor.forClass(String.class);

        configuration.schemaBefore(dataSource(statement), SchemaPropertiesFixture.local()).afterPropertiesSet();

        verify(statement, atLeastOnce()).execute(executed.capture());
        final var sql = executed.getAllValues();
        assertThat(sql).anySatisfy(command -> {
            assertThat(command).contains("CREATE OR REPLACE FUNCTION purify");
            assertThat(command).contains("unaccent");
        });
        assertThat(firstIndex(sql, "FUNCTION purify")).isLessThan(firstIndex(sql, "profile_type"));
    }

    @Test
    @DisplayName("Erro de script lança ScriptException e não executa o próximo")
    void scriptErrorStopsTheNextScript() throws Exception {
        final var statement = statement();
        when(statement.execute(anyString())).thenThrow(new SQLException("connection closed"));
        final var executed = ArgumentCaptor.forClass(String.class);

        final var thrown = catchThrowable(() -> configuration
                .schemaBefore(dataSource(statement), SchemaPropertiesFixture.local())
                .afterPropertiesSet());

        assertThat(thrown).isInstanceOf(ScriptException.class);
        verify(statement, times(1)).execute(executed.capture());
        assertThat(executed.getValue()).doesNotContain("profile_type");
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

    private static Statement statement() throws SQLException {
        final var statement = mock(Statement.class);
        when(statement.execute(anyString())).thenReturn(false);
        when(statement.getUpdateCount()).thenReturn(-1);
        when(statement.getMoreResults()).thenReturn(false);
        return statement;
    }

    private static DataSource dataSource(final Statement statement) throws SQLException {
        final var dataSource = mock(DataSource.class);
        final var connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        return dataSource;
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
