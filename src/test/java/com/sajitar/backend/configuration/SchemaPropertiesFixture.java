package com.sajitar.backend.configuration;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SchemaPropertiesFixture {

    public static final String BEFORE = "classpath:util/functions.sql,classpath:util/enums.sql";

    public static final String AFTER_LOCAL = "util/columns.sql, util/uniques.sql, util/indexes.sql, "
            + "settlement/profile.sql, settlement/checker.sql, settlement/note.sql";

    public static final String AFTER_PROD = "util/columns.sql, util/uniques.sql, util/indexes.sql";

    public static SchemaProperties local() {
        return new SchemaProperties(BEFORE, AFTER_LOCAL);
    }

    public static SchemaProperties prod() {
        return new SchemaProperties(BEFORE, AFTER_PROD);
    }

}
