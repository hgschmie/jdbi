package org.jdbi.v3.caffeine;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.ColonPrefixSqlParser;
import org.jdbi.v3.core.statement.SqlStatements;

/**
 * Installing this plugin restores using the Caffeine cache library for SQL statements and the colon prefix parser.
 */
public class CaffeinePlugin implements JdbiPlugin {

    @Override
    public void customizeJdbi(Jdbi jdbi) {
        final SqlStatements config = jdbi.getConfig(SqlStatements.class);

        config.setTemplateCache(CaffeineCache.builder());
        config.setSqlParser(new ColonPrefixSqlParser(CaffeineCache.builder()));
    }
}
