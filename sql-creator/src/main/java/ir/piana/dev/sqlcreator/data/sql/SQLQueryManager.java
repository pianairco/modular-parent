package ir.piana.dev.sqlcreator.data.sql;

import com.sun.javafx.binding.StringFormatter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import javax.persistence.Column;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mj.rahmati on 12/4/2019.
 */
public class SQLQueryManager {
    private SQLModelManager modelManager;
    private static SQLQueryManager sqlQueryManager;
    private JdbcDaoSupport daoSupport;

//    private Object getValue(String key, HttpServletRequest request, Map<String, Object> paramMap) {
//        Object obj = null;
//        if(request != null) {
//            obj = request.getParameter(key);
//            if (obj == null)
//                obj = request.getAttribute(key);
//            if (obj == null)
//                obj = request.getSession().getAttribute(key);
//            request.getSession().setAttribute(key, obj);
//        }
//        if(paramMap != null && obj == null)
//            obj = paramMap.get(key);
////        if(obj == null)
////            return "";
//        return obj;
//    }
//
//    private Object getValue(String key, String type, HttpServletRequest request, Map<String, Object> paramMap) {
//        Object obj = null;
//        if(request != null) {
//            obj = request.getParameter(key);
//            if (obj == null)
//                obj = request.getAttribute(key);
//            if (obj == null)
//                obj = request.getSession().getAttribute(key);
//            request.getSession().setAttribute(key, obj);
//        }
//        if(paramMap != null && obj == null)
//            obj = paramMap.get(key);
//        if(obj == null) {
//            if(type.equals("string"))
//                return "";
//            else
//                return null;
//        }
//        return obj;
//    }

    private SQLQueryManager(SQLModelManager modelManager) {
        this.modelManager = modelManager;
    }

    public static SQLQueryManager createSQLQueryManager(SQLModelManager sqlModelManager, boolean force) {
        if(force || sqlQueryManager == null)
            sqlQueryManager = new SQLQueryManager(sqlModelManager);
        return sqlQueryManager;
    }

    public static SQLQueryManager getSQLManager() {
        if(sqlQueryManager != null)
            return sqlQueryManager;
        return null;
    }

    public SQLModelManager getSQLModelManager() {
        return modelManager;
    }

    public SQLType getSQLType(String queryName) {
        if(modelManager.hasSelectDef(queryName))
            return SQLType.SELECT;
        else if(modelManager.hasUpdateDef(queryName))
            return SQLType.UPDATE;
        else if(modelManager.hasInsertDef(queryName))
            return SQLType.INSERT;
        else return SQLType.UNKNOWN;
    }

    public String createQuery(String sourceName, ParameterProvider parameterProvider, String parameterPrefix) {
        if(modelManager.hasSelectDef(sourceName))
            return createSelectQuery(sourceName, parameterProvider, parameterPrefix);
        else if(modelManager.hasInsertDef(sourceName))
            return createInsertQuery(sourceName, parameterProvider);
        else if(modelManager.hasUpdateDef(sourceName))
            return createUpdateQuery(sourceName, parameterProvider);
        return null;
    }

    public String createQuery(String sourceName, ParameterProvider parameterProvider) {
        return createQuery(sourceName, parameterProvider, "");
    }

    public String createSelectQuery(String sourceName, ParameterProvider parameterProvider, String parameterPrefix) {
        SelectDef selectDef = modelManager.getSelectDef(sourceName);

        String prefix = parameterPrefix == null || parameterPrefix.isEmpty() ? "" : parameterPrefix.concat(".");
        StringBuffer wheresBuffer = new StringBuffer("");
        String lastConjuction = null;
        if(selectDef.whereParts == null) {

        } else {
            for (Map.Entry<String[], SQLWhereDef> wherePart : selectDef.whereParts) {
                StringBuffer whereBuffer = new StringBuffer("");
                for (int i = 0; i < wherePart.getKey().length - 1; i++) {
                    if (i % 2 == 0)
                        whereBuffer.append(wherePart.getKey()[i]);
                    else {
                        String paramName = wherePart.getKey()[i];

                        if (paramName != null && !paramName.isEmpty()) {
                            Object paramValue = parameterProvider.getValue(prefix.concat(selectDef.paramMap.get(paramName).key));
                            if ((paramValue == null || ((String)paramValue).isEmpty()) && !wherePart.getValue().force)
                                whereBuffer = new StringBuffer("@");
                            else {
                                if (paramValue instanceof List) {
                                    List list = (List) paramValue;
                                    for (Object obj : list)
                                        whereBuffer.append(obj.toString() + ", ");
                                    whereBuffer.deleteCharAt(whereBuffer.length() - 2);
                                } else {
                                    whereBuffer.append(paramValue);
                                }
                            }
                        } else {
                            whereBuffer.append("");
                        }
                    }
                }
                if(whereBuffer.charAt(0) != '@') {
                    lastConjuction = " " + wherePart.getKey()[wherePart.getKey().length - 1] + " ";
                    whereBuffer.append(lastConjuction);
                    wheresBuffer.append(whereBuffer);
                }
            }
        }
        if(wheresBuffer.length() > 0)
            wheresBuffer = new StringBuffer(" where ").append(wheresBuffer);
        if(lastConjuction != null && !lastConjuction.isEmpty())
            wheresBuffer.delete(wheresBuffer.length() - lastConjuction.length(), wheresBuffer.length()).append(" ");
        StringBuffer fromBuffer = new StringBuffer();
        String from = selectDef.fromString;
        if(!from.isEmpty()) {
            for (SelectSourceDef selectSourceDef : selectDef.sources) {
                if(from.contains(" " + selectSourceDef.alias + " ")) {
                    int i = from.indexOf(" " + selectSourceDef.alias + " ");
                    if(selectSourceDef.type != null && selectSourceDef.type.equalsIgnoreCase("source")) {
                        if(i > 16 && from.substring(i - 11, i).equalsIgnoreCase("union all") || (i + selectSourceDef.alias.length() + 11 <= from.length() && from.substring(i + selectSourceDef.alias.length() + 2, i + selectSourceDef.alias.length() + 11).equalsIgnoreCase("union all"))) {
                            from = from.replace(" " + selectSourceDef.alias + " ", " (" + this.createSelectQuery(selectSourceDef.name, parameterProvider, parameterPrefix) + ") ");
                        } else
                            from = from.replace(" " + selectSourceDef.alias + " ", " (" + this.createSelectQuery(selectSourceDef.name, parameterProvider, parameterPrefix) + ") " + selectSourceDef.alias + " ");
                    } else {
                        from = from.replace(" " + selectSourceDef.alias + " ", " " + selectSourceDef.name + " " + selectSourceDef.alias + " ");
                    }
                } else if(from.endsWith(" " + selectSourceDef.alias)) {
                    int j = selectSourceDef.alias.length() + 1;
                    int i = from.length() - j;
                    if(selectSourceDef.type != null && selectSourceDef.type.equalsIgnoreCase("source"))
                        if(from.length() - j - 16 > 0 && from.substring(from.length() - j - 9, from.length() - j).equalsIgnoreCase("union all"))
                            from = from.substring(0, from.length() - j).concat(" (" + this.createSelectQuery(selectSourceDef.name, parameterProvider, parameterPrefix) + ") ");
                        else
                            from = from.substring(0, from.length() - j).concat(" (" + this.createSelectQuery(selectSourceDef.name, parameterProvider, parameterPrefix) + ") " + selectSourceDef.alias + " ");
                    else {
                        from = from.substring(0, from.length() - j).concat(" " + selectSourceDef.name + " " + selectSourceDef.alias + " ");
                    }
                }
            }
            fromBuffer.append(from);
        }

        StringBuffer rightQueryString = new StringBuffer();
        if(selectDef.orderParts != null && !selectDef.orderParts.isEmpty()) {
            rightQueryString.append(" order by ");
            for (Map.Entry<Map.Entry<String, Boolean>, Map.Entry<String, Boolean>> orderPart : selectDef.orderParts) {
                String by = null;
                if(orderPart.getKey().getValue()) {
                    Object paramValue = parameterProvider.getValue(prefix.concat(selectDef.paramMap.get(orderPart.getKey().getKey()).key));
                    if(paramValue == null)
                        continue;
                    by = (String) paramValue;
                } else {
                    by = (String) orderPart.getKey().getKey();
                }
                String order = null;
                if(orderPart.getValue().getValue()) {
                    Object paramValue = parameterProvider.getValue(prefix.concat(selectDef.paramMap.get(orderPart.getValue().getKey()).key));
                    if(paramValue == null)
                        continue;
                    order = (String) paramValue;
                } else {
                    order = (String) orderPart.getValue().getKey();
                }
                rightQueryString.append(by + " " + order + ", ");
            }
            if(rightQueryString.toString().equalsIgnoreCase(" order by "))
                rightQueryString = new StringBuffer("");
            else
                rightQueryString.deleteCharAt(rightQueryString.length() - 2);
        }

        if(selectDef.groups != null && !selectDef.groups.isEmpty()) {
            rightQueryString.append(" group by ");
            for (String group : selectDef.groups)
                rightQueryString.append(group).append(", ");
            rightQueryString.deleteCharAt(rightQueryString.length() - 2);
        }

        Pattern pattern = Pattern.compile("\\$(.*?)\\$");
        Matcher matcher = pattern.matcher(selectDef.leftQueryString);
        String left = selectDef.leftQueryString;
        while (matcher.find()) {
            String group = matcher.group();
            String substring = group.substring(1, group.length() - 1);
            SQLParamDef sqlParamDef = selectDef.paramMap.get(substring);
            String value = (String) parameterProvider.getValue(prefix.concat(sqlParamDef.key), sqlParamDef.type);
            left = left.replaceAll(Pattern.quote(group), value);
        }
        return left.concat(fromBuffer.toString()).concat(wheresBuffer.toString()).concat(rightQueryString.toString()).concat("\n");
    }

    public String createSelectQuery(String sourceName, ParameterProvider parameterProvider) {
        return createSelectQuery(sourceName, parameterProvider, "");
    }

    public String createUpdateQuery(String sourceName, ParameterProvider parameterProvider, String parameterPrefix) {
        UpdateDef updateDef = modelManager.getUpdateDef(sourceName);

        String prefix = parameterPrefix == null || parameterPrefix.isEmpty() ? "" : parameterPrefix.concat(".");

        StringBuffer columnBuffer = new StringBuffer("");
        Pattern pattern = Pattern.compile("\\$(.*?)\\$");
        Pattern sharpPattern = Pattern.compile("\\#(.*?)\\#");
        for(UpdateColumnDef columnDef : updateDef.columns) {
            String statement = columnDef.statement;
            if (statement.contains("$")) {
                Matcher matcher = pattern.matcher(statement);
                while (matcher.find()) {
                    String group = matcher.group();
                    SQLParamDef sqlParamDef = updateDef.paramMap.get(group.substring(1, group.length() - 1));
                    String[] split = statement.split(Pattern.quote(group));
                    StringBuffer part = new StringBuffer(split[0]).append((String) parameterProvider.getValue(prefix.concat(sqlParamDef.key), sqlParamDef.type))/*.append(split[1])*/;
                    columnBuffer.append(part);
                    statement = "";
                    for(int i = 1; i < split.length; i++)
                        statement = statement.concat(split[i]);
                }
                columnBuffer.append(statement).append(", ");
            } else if(statement.contains("#")) {
                Matcher matcher = sharpPattern.matcher(statement);
                while (matcher.find()) {
                    String group = matcher.group();
                    String typeName = group.substring(1, group.length() - 1);
                    String execute = SQLUtilityType.execute(typeName, parameterProvider);
                    String[] split = statement.split(Pattern.quote(group));
                    StringBuffer part = new StringBuffer(split[0]).append(execute);
                    columnBuffer.append(part);
                    statement = "";
                    for(int i = 1; i < split.length; i++)
                        statement = statement.concat(split[i]);
                }
                columnBuffer.append(statement).append(", ");
            } else {
                columnBuffer.append(columnDef.statement).append(", ");
            }
        }
        if(!updateDef.columns.isEmpty())
            columnBuffer.deleteCharAt(columnBuffer.length() - 2);

        StringBuffer wheresBuffer = new StringBuffer("");
        String lastConjuction = null;
        if(updateDef.whereParts != null) {
            for (Map.Entry<String[], SQLWhereDef> wherePart : updateDef.whereParts) {
                StringBuffer whereBuffer = new StringBuffer("");
                for (int i = 0; i < wherePart.getKey().length - 1; i++) {
                    if (i % 2 == 0)
                        whereBuffer.append(wherePart.getKey()[i]);
                    else {
                        String paramName = wherePart.getKey()[i];

                        if (paramName != null && !paramName.isEmpty()) {
                            Object paramValue = parameterProvider.getValue(prefix.concat(updateDef.paramMap.get(paramName).key));
                            if ((paramValue == null || ((String)paramValue).isEmpty()) && !wherePart.getValue().force)
                                whereBuffer = new StringBuffer("@");
                            else {
                                if (paramValue instanceof List) {
                                    List list = (List) paramValue;
                                    for (Object obj : list)
                                        whereBuffer.append(obj.toString() + ", ");
                                    whereBuffer.deleteCharAt(whereBuffer.length() - 2);
                                } else {
                                    whereBuffer.append(paramValue);
                                }
                            }
                        } else {
                            whereBuffer.append("");
                        }
                    }
                }
                if(whereBuffer.charAt(0) != '@') {
                    lastConjuction = " " + wherePart.getKey()[wherePart.getKey().length - 1] + " ";
                    whereBuffer.append(lastConjuction);
                    wheresBuffer.append(whereBuffer);
                }
            }
        }
        if(wheresBuffer.length() > 0)
            wheresBuffer = new StringBuffer(" where ").append(wheresBuffer);
        if(lastConjuction != null && !lastConjuction.isEmpty())
            wheresBuffer.delete(wheresBuffer.length() - lastConjuction.length(), wheresBuffer.length()).append(" ");

        return updateDef.updateQuery.concat(columnBuffer.toString()).concat(wheresBuffer.toString());
    }

    public String createUpdateQuery(String sourceName, ParameterProvider parameterProvider) {
        return createUpdateQuery(sourceName, parameterProvider, "");
    }

    public String createInsertQuery(String sourceName, ParameterProvider parameterProvider, String parameterPrefix) {
        InsertDef insertDef = modelManager.getInsertDef(sourceName);

        String prefix = parameterPrefix == null || parameterPrefix.isEmpty() ? "" : parameterPrefix.concat(".");

        StringBuffer columnBuffer = new StringBuffer("(");
        StringBuffer columnValueBuffer = new StringBuffer(" values (");
        Pattern pattern = Pattern.compile("\\$(.*?)\\$");
        Pattern sharpPattern = Pattern.compile("\\#(.*?)\\#");
        for(InsertColumnDef columnDef : insertDef.columns) {
            columnBuffer.append(columnDef.getName()).append(", ");

            String value = columnDef.value;
            if (value.contains("$")) {
                Matcher matcher = pattern.matcher(value);
                while (matcher.find()) {
                    String group = matcher.group();
                    SQLParamDef sqlParamDef = insertDef.paramMap.get(group.substring(1, group.length() - 1));
                    String[] split = value.split(Pattern.quote(group));
                    if(split.length == 0) {
                        StringBuffer part = new StringBuffer();
                        Object value1 = parameterProvider.getValue(prefix.concat(sqlParamDef.key), sqlParamDef.type);
                        if(sqlParamDef.type.equals("string"))
                            part.append("'").append(value1).append("'");
                        else
                            part.append(value1);
                        columnValueBuffer.append(part);
                        value = "";
                    } else {
                        StringBuffer part = new StringBuffer(split[0]).append((String) parameterProvider.getValue(prefix.concat(sqlParamDef.key), sqlParamDef.type))/*.append(split[1])*/;
                        columnValueBuffer.append(part);
                        value = "";
                        for(int i = 1; i < split.length; i++)
                            value = value.concat(split[i]);
                    }
                }
                columnValueBuffer.append(value).append(", ");
            } else if(value.contains("#")) {
                Matcher matcher = sharpPattern.matcher(value);
                while (matcher.find()) {
                    String group = matcher.group();
                    String typeName = group.substring(1, group.length() - 1);
                    String[] split = value.split(sharpPattern.quote(group));
                    if(split.length == 0) {
                        StringBuffer part = new StringBuffer();
                        String val = SQLUtilityType.execute(typeName, parameterProvider);
                        part.append(val);
                        columnValueBuffer.append(part);
                        value = "";
                    } else {
                        StringBuffer part = new StringBuffer(split[0])
                                .append(SQLUtilityType.execute(split[1], parameterProvider))/*.append(split[1])*/;
                        columnValueBuffer.append(part);
                        value = "";
                        for(int i = 1; i < split.length; i++)
                            value = value.concat(split[i]);
                    }
                }
                columnValueBuffer.append(value).append(", ");
            } else {
                columnValueBuffer.append(columnDef.value).append(", ");
            }
        }
        if(!insertDef.columns.isEmpty()) {
            columnBuffer.deleteCharAt(columnBuffer.length() - 2).append(")");
            columnValueBuffer.deleteCharAt(columnValueBuffer.length() - 2).append(")");
        }

        return insertDef.insertQuery.concat(columnBuffer.toString()).concat(columnValueBuffer.toString());
    }

    public String createInsertQuery(String sourceName, ParameterProvider parameterProvider) {
        return createInsertQuery(sourceName, parameterProvider, "");
    }

    public void query(String queryName, DataSource dataSource, HttpServletRequest request, ParameterProvider parameterProvider, String attributeName)
            throws SQLException, RayanSQLException {
        if(attributeName == null || attributeName.isEmpty())
            attributeName = queryName;
        if(modelManager.hasSelectDef(queryName))
            querySelect(queryName, dataSource, request, parameterProvider, attributeName);
        else if(modelManager.hasInsertDef(queryName))
            queryInsert(queryName, dataSource, request, parameterProvider, attributeName);
        else if(modelManager.hasUpdateDef(queryName))
            queryUpdate(queryName, dataSource, request, parameterProvider, attributeName);
    }

    public void query(String queryName, DataSource dataSource, HttpServletRequest request, ParameterProvider parameterProvider)
            throws SQLException, RayanSQLException {
        query(queryName, dataSource, request, parameterProvider, queryName);
    }

    public void queryUpdate(String queryName, DataSource dataSource, HttpServletRequest request, ParameterProvider parameterProvider, String attributeName)
            throws SQLException, RayanSQLException {
        UpdateDef updateDef = modelManager.getUpdateDef(queryName);

        for(SQLSelectAttribute before : updateDef.befores) {
            querySelect(before.queryName, dataSource, request, parameterProvider, before.attributeName);
        }

        String query = createUpdateQuery(queryName, parameterProvider);
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate(query);

        for(SQLSelectAttribute after : updateDef.afters) {
            query(after.queryName, dataSource, request, parameterProvider, after.attributeName);
        }
    }

    public boolean equality(int value, SQLExecuteIf sqlExecuteIf) {
        if(sqlExecuteIf.evaluation.equals("="))
            return value == Integer.valueOf(sqlExecuteIf.target);
        else if(sqlExecuteIf.evaluation.equals("<="))
            return value <= Integer.valueOf(sqlExecuteIf.target);
        else if(sqlExecuteIf.evaluation.equals(">="))
            return value >= Integer.valueOf(sqlExecuteIf.target);
        else if(sqlExecuteIf.evaluation.equals("<"))
            return value < Integer.valueOf(sqlExecuteIf.target);
        else if(sqlExecuteIf.evaluation.equals(">"))
            return  value > Integer.valueOf(sqlExecuteIf.target);
        else if(sqlExecuteIf.evaluation.equals("!="))
            return  value != Integer.valueOf(sqlExecuteIf.target);
        return false;
    }

    public void queryInsert(String queryName, DataSource dataSource, HttpServletRequest request, ParameterProvider parameterProvider, String attributeName)
            throws SQLException, RayanSQLException {
        InsertDef insertDef = modelManager.getInsertDef(queryName);

        for(SQLExecuteIf sqlExecuteIf : insertDef.sqlExecuteIfs) {
            if(sqlExecuteIf.queryForInt != null && !sqlExecuteIf.queryForInt.isEmpty()) {
                String selectQuery = createSelectQuery(sqlExecuteIf.queryForInt, parameterProvider);
                int i = selectForInt(selectQuery, dataSource);
                if(!equality(i, sqlExecuteIf))
                    return;
            }
        }

        for(SQLSelectAttribute before : insertDef.befores) {
            query(before.queryName, dataSource, request, parameterProvider, before.attributeName);
        }

        String query = createInsertQuery(queryName, parameterProvider);
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
//        String insertQuery = sqlQueryManager.createQuery(queryName, request, parameterProvider);
        statement.execute(query);

        for(SQLSelectAttribute after : insertDef.afters) {
            query(after.queryName, dataSource, request, parameterProvider, after.attributeName);
        }
    }

    public int selectForInt(String query, DataSource dataSource)
            throws RayanSQLException{
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            Integer anInt = null;
            if(resultSet.next())
                anInt = resultSet.getInt(1);
            return anInt;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RayanSQLException(StringFormatter.format("select for int {0} not completed", query).toString());
        }
    }

    public void querySelect(String queryName, DataSource dataSource, HttpServletRequest request, ParameterProvider parameterProvider, String attributeName)
            throws SQLException, RayanSQLException {
        SelectDef selectDef = modelManager.getSelectDef(queryName);

        for(SQLExecuteIf sqlExecuteIf : selectDef.sqlExecuteIfs) {
            if(sqlExecuteIf.queryForInt != null && !sqlExecuteIf.queryForInt.isEmpty()) {
                String selectQuery = createSelectQuery(sqlExecuteIf.queryForInt, parameterProvider);
                int i = selectForInt(selectQuery, dataSource);
                if(!equality(i, sqlExecuteIf))
                    return;
            }
        }

        for(SQLSelectAttribute before : selectDef.befores) {
            querySelect(before.queryName, dataSource, request, parameterProvider, before.attributeName);
        }

        String query = createSelectQuery(queryName, parameterProvider);
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

                if(resultSet != null) {
            if (selectDef.selectType.equals("int")) {
                if (resultSet.next())
                    request.setAttribute(attributeName, resultSet.getInt(1));
            } else if (selectDef.selectType.equals("result-set")) {
                List<Map<String, String>> items = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, String> map = new LinkedHashMap<>();
                    for (SelectColumnDef selectColumnDef : selectDef.columns) {
                        map.put(selectColumnDef.as, resultSet.getString(selectColumnDef.as));
                    }
                    items.add(map);
                }
                request.setAttribute(attributeName, items);
            }/* else if (selectDef.selectType.equals("fill-request")) {
                List<Map<String, String>> items = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, String> map = new LinkedHashMap<>();
                    for (SelectColumnDef selectColumnDef : selectDef.columns) {
                        map.put(selectColumnDef.as, resultSet.getString(selectColumnDef.as));
                        request.setAttribute(selectColumnDef.as, resultSet.getString(selectColumnDef.as));
                    }
                    items.add(map);
                }
                request.setAttribute(attributeName, items);
            }*/

            for(SQLSelectAttribute after : selectDef.afters) {
                query(after.queryName, dataSource, request, parameterProvider, after.attributeName);
            }
        }
    }

    public List<Map<String, Object>> querySelect(String query, DataSource dataSource)
            throws SQLException, RayanSQLException {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        List<Map<String, Object>> items = new ArrayList<>();
        if(resultSet != null) {
            while (resultSet.next()) {
                Map<String, Object> map = new TreeMap<String, Object>(SQLExecuter.CaseInsensitiveComparator.INSTANCE);
                ResultSetMetaData metaData = resultSet.getMetaData();
                for (int i = 1; i < metaData.getColumnCount(); i++) {
                    map.put(metaData.getColumnName(i).toLowerCase(), resultSet.getObject(metaData.getColumnName(i)));
                }
                items.add(map);
            }
        }
        return items;
    }

    private <T> Method getSetMethod(Class<T> targetClazz, Method method, Class parameterType)
            throws NoSuchMethodException {
        Method setdMethod = null;
        if (method.getName().startsWith("get")) {
            setdMethod = targetClazz.getDeclaredMethod(
                    "set".concat(method.getName().substring(3)), parameterType);
        } else if (method.getName().startsWith("is")) {
            setdMethod = targetClazz.getDeclaredMethod(
                    "set".concat(method.getName().substring(2)), parameterType);
        }
        return setdMethod;
    }

    private <T> List<T> getResultSetAsListOfEntity(ResultSet resultSet, Class<T> entityClazz) throws RayanSQLException {
        List<T> list = new ArrayList<>();
        Method[] methods = entityClazz.getDeclaredMethods();
        try {
            while (resultSet.next()) {
                T targetObject = null;

                targetObject = entityClazz.newInstance();
                for (Method method : methods) {
                    Column column = null;
                    if ((column = method.getAnnotation(Column.class)) != null) {
                        try {
                            Method setMethod = null;
                            Class<?> parameterType = method.getReturnType();
                            if (parameterType == String.class) {
                                setMethod = getSetMethod(entityClazz, method, String.class);
                                setMethod.invoke(targetObject, resultSet.getString(column.name()));
                            } else if (parameterType == Timestamp.class) {
                                setMethod = getSetMethod(entityClazz, method, Timestamp.class);
                                Timestamp timestamp = resultSet.getTimestamp(column.name());
                                setMethod.invoke(targetObject, timestamp);
                            } else if (parameterType == Integer.class) {
                                setMethod = getSetMethod(entityClazz, method, Integer.class);
                                int i = resultSet.getInt(column.name());
                                setMethod.invoke(targetObject, i);
                            } else if (parameterType == int.class) {
                                setMethod = getSetMethod(entityClazz, method, int.class);
                                int i = resultSet.getInt(column.name());
                                setMethod.invoke(targetObject, i);
                            } else if (parameterType == Boolean.class) {
                                setMethod = getSetMethod(entityClazz, method, Boolean.class);
                                Boolean i = resultSet.getBoolean(column.name());
                                setMethod.invoke(targetObject, i);
                            } else if (parameterType == boolean.class) {
                                setMethod = getSetMethod(entityClazz, method, boolean.class);
                                boolean i = resultSet.getBoolean(column.name());
                                setMethod.invoke(targetObject, i);
                            } else if (parameterType == Long.class || parameterType == long.class) {
                                setMethod = getSetMethod(entityClazz, method, Long.class);
                                long l = resultSet.getLong(column.name());
                                setMethod.invoke(targetObject, l);
                            }
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
                list.add(targetObject);
            }
        } catch (Exception e) {
            throw new RayanSQLException(e.getMessage());
        }
        return list;
    }

    public <T> List<T> select(String queryName, DataSource dataSource, ParameterProvider parameterProvider)
            throws RayanSQLException {
        Class entityClazz = modelManager.getSelectDef(queryName).entityClazz;
        if(entityClazz == null)
            return new ArrayList<>();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        List<T> list = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            String selectQuery = createSelectQuery(queryName, parameterProvider);
            resultSet = statement.executeQuery(selectQuery);
            if (resultSet != null)
                list = getResultSetAsListOfEntity(resultSet, entityClazz);
        } catch (Exception e) {
            throw new RayanSQLException(e.getMessage());
        } finally {
            try {
                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                throw new RayanSQLException(e.getMessage());
            }
        }
        return list != null ? list : new ArrayList<>();
    }

    public List<String> getAllSelectQueryNames() {
        return getSQLModelManager().getAllSelectQueryNames();
    }

    public List<String> getAllUpdateQueryNames() {
        return getSQLModelManager().getAllUpdateQueryNames();
    }

    public List<String> getAllInsertQueryNames() {
        return getSQLModelManager().getAllInsertQueryNames();
    }

    public List<SQLParamDef> getSQLParams(String sourceName) {
        return getSQLModelManager().getSQLParams(sourceName);
    }

    public SQLModelManager.SourceFlow getSourceFlow(String sourceName) {
        return getSQLModelManager().getSourceFlow(sourceName);
    }

    public List<String> getParents(String sourceName) {
        return getSQLModelManager().getParents(sourceName);
    }
}
