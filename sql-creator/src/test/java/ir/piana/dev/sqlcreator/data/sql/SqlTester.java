package ir.piana.dev.sqlcreator.data.sql;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

public class SqlTester {
    @Test
    public void testSqlCreator() throws Exception {
        SQLModelManager sqlModelManager = SQLModelManager.getNewInstance(
                SqlTester.class.getResourceAsStream("/query.json"));
        SQLQueryManager sqlQueryManager = SQLQueryManager
                .createSQLQueryManager(sqlModelManager, false);

        String query = sqlQueryManager.createQuery(
                "select-branch-list", new DefaultParameterProvider(new LinkedHashMap<>()));
        System.out.println(query);
        Assert.assertEquals("select b.branch_id branchId, b.branch_name branchName  from  branch b  order by b.branch_id asc \n", query);
    }
}
