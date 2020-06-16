package ir.piana.dev.sqlcreator.data.sql;

import javax.persistence.Column;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

/**
 * Created by mj.rahmati on 12/4/2019.
 */
public class SQLExecuter {
//    private SQLModelManager modelManager;
//    private SQLQueryManager sqlQueryManager;
    private static SQLExecuter sqlExecuter;

    public static SQLExecuter getInstance() {
        return new SQLExecuter();
    }

    public void query(String query, SQLType sqlType, Statement statement, HttpServletRequest request, ParameterProvider parameterProvider, String attributeName)
            throws SQLException, RayanSQLException {
        if(sqlType == SQLType.SELECT)
            querySelect(query, statement, request, parameterProvider, attributeName);
        else if(sqlType == SQLType.SELECT_FOR_INT)
            querySelectForInt(query, statement, request, parameterProvider, attributeName);
        else if(sqlType == SQLType.INSERT)
            queryInsertOrUpdate(query, statement, request, parameterProvider, attributeName);
        else if(sqlType == SQLType.UPDATE)
            queryInsertOrUpdate(query, statement, request, parameterProvider, attributeName);
    }

    public void querySelect(String query, Statement statement, HttpServletRequest request, ParameterProvider parameterProvider, String attributeName)
            throws SQLException, RayanSQLException {
//        ResultSet resultSet = statement.executeQuery(query);
//        if(resultSet != null) {
//            List<Map<String, String>> items = new ArrayList<>();
//            while (resultSet.next()) {
//                Map<String, String> map = new LinkedHashMap<>();
//                ResultSetMetaData metaData = resultSet.getMetaData();
//                for (int i = 1; i < metaData.getColumnCount(); i++) {
//                    map.put(metaData.getColumnName(i), resultSet.getString(metaData.getColumnName(i)));
//                }
//                items.add(map);
//            }

        List<Map<String, Object>> items = querySelect(query, statement, request, parameterProvider);
        request.setAttribute(attributeName, items);
//            return items;
//        }
//        List<Map<String, String>> items = querySelect(query, statement, request, parameterProvider);
//        request.setAttribute(attributeName, items);
    }

    public List<Map<String, Object>> querySelect(String query, Statement statement, HttpServletRequest request, ParameterProvider parameterProvider)
            throws SQLException, RayanSQLException {
        ResultSet resultSet = statement.executeQuery(query);
        if(resultSet != null) {
            List<Map<String, Object>> items = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, Object> map = new TreeMap<String, Object>(CaseInsensitiveComparator.INSTANCE);
                ResultSetMetaData metaData = resultSet.getMetaData();
                for (int i = 1; i < metaData.getColumnCount(); i++) {
                    map.put(metaData.getColumnName(i).toLowerCase(), resultSet.getObject(metaData.getColumnName(i)));
                }
                items.add(map);
            }
            return items;
        }
        return new ArrayList<>();
    }

    public static class CaseInsensitiveComparator implements Comparator<String> {
        public static final CaseInsensitiveComparator INSTANCE =
                new CaseInsensitiveComparator();

        public int compare(String first, String second) {
            // some null checks
            return first.compareToIgnoreCase(second);
        }
    }

    public void querySelectForInt(String query, Statement statement, HttpServletRequest request, ParameterProvider parameterProvider, String attributeName)
            throws SQLException, RayanSQLException {
        ResultSet resultSet = statement.executeQuery(query);
        Integer anInt = null;
        if(resultSet.next())
            anInt = resultSet.getInt(1);
        request.setAttribute(attributeName, anInt);
    }

    public void queryInsertOrUpdate(String query, Statement statement, HttpServletRequest request, ParameterProvider parameterProvider, String attributeName)
            throws SQLException, RayanSQLException {
        statement.executeUpdate(query);
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

    public <T> List<T> select(String query, Class entityClazz, Statement statement, ParameterProvider parameterProvider)
            throws SQLException, RayanSQLException  {
        if(entityClazz == null)
            return new ArrayList<>();
        ResultSet resultSet = null;
        List<T> list = null;
        resultSet = statement.executeQuery(query);
        if (resultSet != null)
            list = getResultSetAsListOfEntity(resultSet, entityClazz);
        return list != null ? list : new ArrayList<>();
    }
}
