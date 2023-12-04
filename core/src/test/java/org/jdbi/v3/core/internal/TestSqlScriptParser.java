package org.jdbi.v3.core.internal;

import java.util.List;

import com.google.common.base.Joiner;
import org.antlr.v4.runtime.CharStreams;
import org.jdbi.v3.core.internal.SqlScriptParser.ScriptTokenHandler;
import org.junit.jupiter.api.Test;

class TestSqlScriptParser {

    @Test
    void testIssue2554() {
        String sqlScript = "CREATE PROCEDURE QWE()\n" +
            "BEGIN\n" +
            "END;\n" +
            "SELECT 1 FROM DUAL;\n" +
            "SELECT 1 FROM DUAL;\n" +
            "SELECT 1 FROM DUAL;\n" +
            "SELECT 1 FROM DUAL;\n";

        List<String> result = parseString(sqlScript);
        System.out.println(Joiner.on("**\n**").join(result));
    }


    private List<String> parseString(String input) {
        ScriptTokenHandler scriptTokenHandler = new ScriptTokenHandler();
        String lastStatement = new SqlScriptParser(scriptTokenHandler)
            .parse(CharStreams.fromString(input));

        return scriptTokenHandler.addStatement(lastStatement);
    }
}
