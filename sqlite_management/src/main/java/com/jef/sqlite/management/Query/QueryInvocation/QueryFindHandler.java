package com.jef.sqlite.management.Query.QueryInvocation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jef.sqlite.management.SQLiteManagement;
import com.jef.sqlite.management.exceptions.SQLiteException;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.SQLiteQuery;
import com.jef.sqlite.management.interfaces.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Handler class for find operations in the query system.
 * This class contains all methods related to finding and querying entities.
 * Supports dynamic queries with AND, OR, and OrderBy clauses.
 * 
 * @param <T> The entity type being queried
 */
public class QueryFindHandler<T> {

    private final Class<T> entityClass;
    private final SQLiteManagement management;
    private final String tableName;
    private final Map<String, String> fieldToColumn;

    // Constants for query parsing
    private static final String AND_OPERATOR = "And";
    private static final String OR_OPERATOR = "Or";
    private static final String ORDER_BY_CLAUSE = "OrderBy";
    private static final String ASC_SUFFIX = "Asc";
    private static final String DESC_SUFFIX = "Desc";

    // Patterns for method name parsing
    private static final Pattern FIND_PATTERN = Pattern.compile("^find(All)?(By)?(.*)$");
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("(.*)OrderBy([A-Z][a-zA-Z0-9]*)(Asc|Desc)?$");


    /**
     * Constructor for QueryFindHandler
     * 
     * @param entityClass The entity class being queried
     * @param management The SQLiteManagement instance
     */
    public QueryFindHandler(Class<T> entityClass, SQLiteManagement management) {
        this.entityClass = entityClass;
        this.management = management;
        this.tableName = entityClass.getAnnotation(Table.class).name();
        this.fieldToColumn = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields())
            if (field.isAnnotationPresent(Column.class))
                fieldToColumn.put(field.getName().toLowerCase(), field.getAnnotation(Column.class).name());
    }

    /**
     * Main method to handle all find operations.
     * This method parses the method name and delegates to the appropriate handler.
     * 
     * @param method The method being invoked
     * @param args The arguments passed to the method
     * @return The result of the query (entity, list of entities, or Optional)
     * @throws SQLiteException If there's an error in the query
     */
    public Object handleFindMethod(Method method, Object[] args) {
        // Check for SQLiteQuery annotation first
        if (method.isAnnotationPresent(SQLiteQuery.class)) {
            return executeCustomQuery(method, args);
        }

        String methodName = method.getName();

        // Handle findAll method directly
        if (methodName.equals("findAll"))
            return findAll();

        // For backward compatibility with existing methods
        if (methodName.matches("findAllOrderBy\\w+(Asc|Desc)?"))
            return findAllOrderBy(method);

        if (methodName.matches("findAllBy\\w+OrderBy\\w+(Asc|Desc)?") && args != null && args.length > 0)
            return findAllByFieldOrderByField(method, args);

        if (methodName.startsWith("findBy") && !methodName.contains(AND_OPERATOR) && !methodName.contains(OR_OPERATOR))
            return createFindBy(method, args);

        if (methodName.startsWith("findAllBy") && !methodName.contains(ORDER_BY_CLAUSE) && 
            !methodName.contains(AND_OPERATOR) && !methodName.contains(OR_OPERATOR))
            return createFindAllBy(method, args);


        // For new dynamic queries
        // Parse the method name to extract query components
        QueryComponents components = parseMethodName(methodName);

        // Build and execute the query
        return executeQuery(components, method, args);
    }

    /**
     * Parses a method name to extract query components.
     * 
     * @param methodName The name of the method to parse
     * @return A QueryComponents object containing the parsed components
     * @throws SQLiteException If the method name is invalid
     */
    private QueryComponents parseMethodName(String methodName) {
        Matcher findMatcher = FIND_PATTERN.matcher(methodName);
        if (!findMatcher.matches()) {
            throw new SQLiteException("Invalid method name format: " + methodName);
        }

        boolean findAll = findMatcher.group(1) != null;
        String conditions = findMatcher.group(3);

        QueryComponents components = new QueryComponents(findAll);

        // Check for OrderBy clause
        Matcher orderByMatcher = ORDER_BY_PATTERN.matcher(methodName);
        if (orderByMatcher.matches()) {
            String orderByField = orderByMatcher.group(2);
            String direction = orderByMatcher.group(3);

            // Convert to camelCase for field lookup
            String orderByFieldLower = Character.toLowerCase(orderByField.charAt(0)) + orderByField.substring(1);

            // Check if the field exists
            String orderColumn = fieldToColumn.get(orderByFieldLower);
            if (orderColumn == null) {
                throw new SQLiteException("Order field not found: " + orderByFieldLower);
            }

            components.setOrderBy(orderColumn, direction == null || !direction.equals(DESC_SUFFIX));

            // Remove OrderBy clause from conditions
            conditions = orderByMatcher.group(1);
            if (conditions.startsWith("By")) {
                conditions = conditions.substring(2);
            }
        }

        // If there are no conditions, return the components
        if (conditions.isEmpty() || !conditions.startsWith("By")) {
            return components;
        }

        // Remove the "By" prefix
        conditions = conditions.substring(2);

        // Parse conditions (fields and operators)
        parseConditions(conditions, components);

        return components;
    }

    /**
     * Parses the conditions part of a method name and adds them to the QueryComponents.
     * 
     * @param conditions The conditions part of the method name
     * @param components The QueryComponents object to add the conditions to
     * @throws SQLiteException If a field doesn't exist
     */
    private void parseConditions(String conditions, QueryComponents components) {
        // Split by "And" and "Or" operators
        List<String> conditionParts = new ArrayList<>();
        List<String> operators = new ArrayList<>();

        int startIndex = 0;
        for (int i = 1; i < conditions.length(); i++) {
            if (i + 2 < conditions.length() && 
                Character.isUpperCase(conditions.charAt(i)) && 
                conditions.substring(i, i + 3).equals(AND_OPERATOR)) {
                conditionParts.add(conditions.substring(startIndex, i));
                operators.add(AND_OPERATOR);
                startIndex = i + 3;
                i += 2;
            } else if (i + 1 < conditions.length() && 
                       Character.isUpperCase(conditions.charAt(i)) && 
                       conditions.substring(i, i + 2).equals(OR_OPERATOR)) {
                conditionParts.add(conditions.substring(startIndex, i));
                operators.add(OR_OPERATOR);
                startIndex = i + 2;
                i += 1;
            }
        }

        // Add the last part
        conditionParts.add(conditions.substring(startIndex));

        // Process each condition
        int argIndex = 0;
        for (int i = 0; i < conditionParts.size(); i++) {
            String condition = conditionParts.get(i);
            String operator = i < operators.size() ? operators.get(i) : null;

            // Convert to camelCase for field lookup
            String fieldLower = Character.toLowerCase(condition.charAt(0)) + condition.substring(1);

            // Check if the field exists
            String column = fieldToColumn.get(fieldLower);
            if (column == null) {
                throw new SQLiteException("Field not found: " + fieldLower);
            }

            components.addCondition(column, operator, argIndex++);
        }
    }

    /**
     * Builds and executes a query based on the parsed components.
     * 
     * @param components The parsed query components
     * @param method The method being invoked
     * @param args The arguments passed to the method
     * @return The result of the query
     * @throws SQLiteException If there's an error in the query
     */
    private Object executeQuery(QueryComponents components, Method method, Object[] args) {
        if (args == null) {
            args = new Object[0];
        }

        // Validate that we have enough arguments for the conditions
        int requiredArgs = components.getConditions().size();
        if (args.length < requiredArgs) {
            throw new SQLiteException("Not enough arguments for query. Expected " + requiredArgs + 
                                     " but got " + args.length);
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);

        // Add WHERE clause if there are conditions
        List<QueryCondition> conditions = components.getConditions();
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ");

            for (int i = 0; i < conditions.size(); i++) {
                QueryCondition condition = conditions.get(i);

                if (i > 0) {
                    // Add the operator (AND/OR)
                    String previousOperator = conditions.get(i - 1).getOperator();
                    if (OR_OPERATOR.equals(previousOperator)) {
                        sql.append(" OR ");
                    } else {
                        sql.append(" AND ");
                    }
                }

                sql.append(condition.getField()).append(" = ?");
            }
        }

        // Add ORDER BY clause if specified
        if (components.hasOrderBy()) {
            sql.append(" ORDER BY ")
               .append(components.getOrderByField())
               .append(components.isOrderAscending() ? " ASC" : " DESC");
        }

        // Execute the query
        String sqlQuery = sql.toString();

        // Create an array of arguments in the correct order for the query
        Object[] queryArgs = new Object[conditions.size()];
        for (int i = 0; i < conditions.size(); i++) {
            QueryCondition condition = conditions.get(i);
            int argIndex = condition.getArgIndex();
            if (argIndex < args.length) {
                queryArgs[i] = args[argIndex];
            }
        }

        // Check the return type of the method
        Class<?> returnType = method.getReturnType();

        if (components.hasFindAll() || List.class.isAssignableFrom(returnType)) {
            return queryList(sqlQuery, queryArgs);
        } else {
            return queryItem(sqlQuery, createArgs(queryArgs));
        }
    }

    /**
     * Crea una consulta para buscar entidades por un valor específico de un campo.
     * Analiza el nombre del metodo para determinar el campo por el que buscar.
     * 
     * @param method El metodo invocado
     * @param args Los argumentos pasados al metodo (valor a buscar)
     * @return Una entidad o lista de entidades que coinciden con el criterio
     * @throws SQLiteException Si el campo no existe o hay errores en la consulta
     */
    public Object createFindBy(Method method, Object[] args) {
        String methodName = method.getName();

        String field = methodName.substring("findBy".length());
        String fieldLower = Character.toLowerCase(field.charAt(0)) + field.substring(1);

        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);

        String sql = "SELECT * FROM " + tableName + " WHERE " + column + " = ?";

        // Check the return type of the method
        Class<?> returnType = method.getReturnType();
        if (List.class.isAssignableFrom(returnType)) {
            return queryList(sql, args);
        } else {
            return queryItem(sql, createArgs(args));
        }
    }

    /**
     * Crea una consulta para buscar todas las entidades que coinciden con un valor específico de un campo.
     * Analiza el nombre del metodo para determinar el campo por el que buscar.
     * 
     * @param method El metodo invocado
     * @param args Los argumentos pasados al metodo (valor a buscar)
     * @return Una lista de entidades que coinciden con el criterio
     * @throws SQLiteException Si el campo no existe o hay errores en la consulta
     */
    public List<T> createFindAllBy(Method method, Object[] args) {
        String methodName = method.getName();

        String field = methodName.substring("findAllBy".length());
        String fieldLower = Character.toLowerCase(field.charAt(0)) + field.substring(1);

        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);

        String sql = "SELECT * FROM " + tableName + " WHERE " + column + " = ?";
        return queryList(sql, args);
    }

    /**
     * Retrieves all entities from the database.
     * 
     * @return A list of all entities
     */
    public List<T> findAll() {
        String sql = "SELECT * FROM " + tableName;
        return queryList(sql, new String[0]);
    }

    /**
     * Retrieves all entities ordered by a specific field.
     * The direction (ASC or DESC) is determined from the method name.
     * 
     * @param method The method being invoked
     * @return A list of all entities ordered by the specified field
     * @throws SQLiteException If the field doesn't exist or there's an error in the query
     */
    public List<T> findAllOrderBy(Method method) {
        String methodName = method.getName();

        // Extract the direction from the method name
        String direction = "ASC"; // Default to ASC
        if (methodName.endsWith("Desc")) {
            direction = "DESC";
            methodName = methodName.substring(0, methodName.length() - 4); // Remove "Desc"
        } else if (methodName.endsWith("Asc")) {
            methodName = methodName.substring(0, methodName.length() - 3); // Remove "Asc"
        }

        // Extract the field name
        String field = methodName.substring("findAllOrderBy".length());
        String fieldLower = field.toLowerCase();

        String column = fieldToColumn.get(fieldLower);
        if (column == null)
            throw new SQLiteException("Field not found: " + fieldLower);

        String sql = "SELECT * FROM " + tableName + " ORDER BY " + column + " " + direction;

        return queryList(sql, new String[0]);
    }

    /**
     * Retrieves all entities that match a specific field value, ordered by another field.
     * The direction (ASC or DESC) is determined from the method name.
     * 
     * @param method The method being invoked
     * @param args The arguments passed to the method (field value only)
     * @return A list of entities that match the criteria, ordered as specified
     * @throws SQLiteException If the fields don't exist or there's an error in the query
     */
    public List<T> findAllByFieldOrderByField(Method method, Object[] args) {
        String methodName = method.getName();

        // Extract the direction from the method name
        String direction = "ASC"; // Default to ASC
        if (methodName.endsWith("Desc")) {
            direction = "DESC";
            methodName = methodName.substring(0, methodName.length() - 4); // Remove "Desc"
        } else if (methodName.endsWith("Asc")) {
            methodName = methodName.substring(0, methodName.length() - 3); // Remove "Asc"
        }

        // Extract the field names from the method name
        String[] parts = methodName.split("OrderBy");
        if (parts.length != 2)
            throw new SQLiteException("Invalid method name format: " + methodName);

        String whereFieldPart = parts[0].substring("findAllBy".length());
        String orderFieldPart = parts[1];

        // Convert to camelCase for field lookup
        String whereFieldLower = Character.toLowerCase(whereFieldPart.charAt(0)) + whereFieldPart.substring(1);
        String orderFieldLower = Character.toLowerCase(orderFieldPart.charAt(0)) + orderFieldPart.substring(1);

        // Get the actual column names
        String whereColumn = fieldToColumn.get(whereFieldLower);
        if (whereColumn == null)
            throw new SQLiteException("Where field not found: " + whereFieldLower);

        String orderColumn = fieldToColumn.get(orderFieldLower);
        if (orderColumn == null)
            throw new SQLiteException("Order field not found: " + orderFieldLower);

        // Build the SQL query
        String sql = "SELECT * FROM " + tableName + " WHERE " + whereColumn + " = ? ORDER BY " + orderColumn + " " + direction;

        // Execute the query with just the first argument (the where value)
        return queryList(sql, createArgs(args));
    }

    /**
     * Executes a SQL query and returns a list of entities.
     * 
     * @param sql The SQL query to execute
     * @param args The arguments for the query
     * @return A list of entities that match the query
     * @throws SQLiteException If there's an error executing the query
     */
    public List<T> queryList(String sql, Object[] args) {
        SQLiteDatabase db = management.getReadableDatabase();
        List<T> results = new ArrayList<>();

        try {
            Cursor cursor = db.rawQuery(sql, createArgs(args));
            while (cursor.moveToNext())
                results.add((T) getResultCursor(cursor, entityClass));

            cursor.close();
        } catch (Exception ex) {
            throw new SQLiteException("Error executing query: " + ex.getMessage(), ex);
        } finally {
            db.close();
        }

        return results;
    }

    /**
     * Executes a SQL query and returns a single entity.
     * 
     * @param sql The SQL query to execute
     * @param selectionArgs The arguments for the query
     * @return An Optional containing the entity if found, or empty if not found
     * @throws SQLiteException If there's an error executing the query
     */
    public Optional<T> queryItem(String sql, String[] selectionArgs) {
        SQLiteDatabase db = management.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery(sql, selectionArgs);
            if (cursor.moveToFirst())
                return Optional.of((T) getResultCursor(cursor, entityClass));
        } catch (Exception ex) {
            throw new SQLiteException("Error executing query: " + ex.getMessage(), ex);
        } finally {
            db.close();
        }

        return Optional.empty();
    }

    /**
     * Creates an entity instance from a database cursor.
     * 
     * @param cursor The database cursor positioned at the row to read
     * @return An instance of the entity populated with data from the cursor
     * @throws SQLiteException If there's an error creating the entity
     */
    public Object getResultCursor(Cursor cursor, Class<?> entityClass) {
        try {
            Object instance = entityClass.newInstance();

            // Process all fields
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class))
                    processColumnField(cursor, instance, field);
                else if (field.isAnnotationPresent(Join.class))
                    processJoinField(cursor, instance, field);

            }

            return instance;
        } catch (Exception e) {
            throw new SQLiteException("Error creating entity from cursor: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a field annotated with @Column, setting its value from the cursor.
     * 
     * @param cursor The database cursor
     * @param instance The entity instance
     * @param field The field to process
     * @throws Exception If there's an error processing the field
     */
    private void processColumnField(Cursor cursor, Object instance, Field field) throws Exception {
        Column column = field.getAnnotation(Column.class);
        String columnName = column.name();
        int columnIndex = cursor.getColumnIndex(columnName);

        if (columnIndex == -1)
            return;

        field.setAccessible(true);
        Class<?> fieldType = field.getType();

        if (cursor.isNull(columnIndex)) {
            field.set(instance, null);
            return;
        }

        if (fieldType == String.class)
            field.set(instance, cursor.getString(columnIndex));
        else if (fieldType == int.class || fieldType == Integer.class)
            field.set(instance, cursor.getInt(columnIndex));
        else if (fieldType == long.class || fieldType == Long.class)
            field.set(instance, cursor.getLong(columnIndex));
        else if (fieldType == double.class || fieldType == Double.class)
            field.set(instance, cursor.getDouble(columnIndex));
        else if (fieldType == float.class || fieldType == Float.class)
            field.set(instance, cursor.getFloat(columnIndex));
        else if (fieldType == boolean.class || fieldType == Boolean.class)
            field.set(instance, cursor.getInt(columnIndex) == 1);

    }

    /**
     * Processes a field annotated with @Join, loading the related entity.
     * 
     * @param instance The entity instance
     * @param field The field to process
     * @throws Exception If there's an error processing the field
     */
    private void processJoinField(Cursor cursor, Object instance, Field field) throws Exception {
        Join join = field.getAnnotation(Join.class);
        Class<?> relationshipClass = join.relationShip();

        if (relationshipClass == null)
            throw new SQLiteException("Relationship class not specified for @Join field: " + field.getName());
        if (relationshipClass.getAnnotation(Table.class) == null)
            throw new SQLiteException("Relationship class is not annotated with @Table: " + relationshipClass.getName());

        Field fieldJoin = Stream.of(relationshipClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .filter(f -> f.getName().equalsIgnoreCase(join.source()))
                .findFirst()
                .orElse(null);

        String sourceColumnName = fieldJoin.getAnnotation(Column.class).name();

        int columnIndex = cursor.getColumnIndex(join.targetName());

        if (columnIndex == -1)
            return;

        if (cursor.isNull(columnIndex))
            return;

        String targetNameValue = cursor.getString(columnIndex);

        // Create a query to find the related entity
        final String sql = "SELECT * FROM " + relationshipClass.getAnnotation(Table.class).name() +
                " WHERE " + sourceColumnName + " = ?";

        // Execute the query
        SQLiteDatabase db = management.getReadableDatabase();
        try {
            Cursor cursorJoin = db.rawQuery(sql, new String[] { targetNameValue });
            if (cursorJoin.moveToFirst()) {

                Object relatedInstance = getResultCursor(cursorJoin, relationshipClass);

                // Set the related instance in the main entity
                field.setAccessible(true);
                field.set(instance, relatedInstance);
            }

            cursorJoin.close();
        } finally {
            db.close();
        }
    }


    /**
     * Executes a custom SQL query defined in a SQLiteQuery annotation.
     * 
     * @param method The method annotated with SQLiteQuery
     * @param args The arguments passed to the method
     * @return The result of the query, either a List<T> or Optional<T> depending on the method's return type
     * @throws SQLiteException If there's an error executing the query
     */
    public Object executeCustomQuery(Method method, Object[] args) {
        // Get the SQL query from the annotation
        SQLiteQuery annotation = method.getAnnotation(SQLiteQuery.class);
        if (annotation == null)
            throw new SQLiteException("Method is not annotated with SQLiteQuery: " + method.getName());

        String sql = annotation.sql();
        if (sql == null || sql.isEmpty())
            throw new SQLiteException("SQL query cannot be empty in SQLiteQuery annotation");

        final String[] queryArgs = createArgs(args);

        // Check the return type and execute the appropriate query
        Class<?> returnType = method.getReturnType();
        if (List.class.isAssignableFrom(returnType)) {
            return queryList(sql, queryArgs);
        } else if (Optional.class.isAssignableFrom(returnType)) {
            return queryItem(sql, queryArgs);
        } else {
            throw new SQLiteException("Unsupported return type for SQLiteQuery: " + returnType.getName() + 
                                     ". Must be List<T> or Optional<T>");
        }
    }

    public String[] createArgs(Object[] args) {
        if (args == null || args.length == 0)
            return new String[0];

    	String[] result = new String[args.length];
    	for(int i = 0; i < args.length; i++) {
            String typeName = args[i].getClass().getSimpleName().toLowerCase();

            switch (typeName) {
                case "string":
                case "short":
                case "int":
                case "integer":
                case "long":
                case "double":
                case "float":
                    result[i] = args[i].toString();
                    break;
                case "boolean":
                    boolean value = (boolean) args[i];
                    result[i] = value ? "1" : "0";
                    break;

                default:
                    throw new SQLiteException("Unsupported type for parameter: " + args[i].getClass().getSimpleName());
            }

    	}

    	return result;
    }



    /**
     * Class to hold the components of a query parsed from a method name.
     */
    private static class QueryComponents {

        private final boolean findAll;
        private final List<QueryCondition> conditions = new ArrayList<>();
        private String orderByField;
        private boolean orderAscending = true;

        public QueryComponents(boolean findAll) {
            this.findAll = findAll;
        }

        public void addCondition(String field, String operator, int argIndex) {
            conditions.add(new QueryCondition(field, operator, argIndex));
        }

        public void setOrderBy(String field, boolean ascending) {
            this.orderByField = field;
            this.orderAscending = ascending;
        }

        public boolean hasFindAll() {
            return findAll;
        }

        public List<QueryCondition> getConditions() {
            return conditions;
        }

        public boolean hasOrderBy() {
            return orderByField != null;
        }

        public String getOrderByField() {
            return orderByField;
        }

        public boolean isOrderAscending() {
            return orderAscending;
        }


    }

    /**
     * Class to hold a single condition in a query.
     */
    private static class QueryCondition {

        private final String field;
        private final String operator;
        private final int argIndex;

        public QueryCondition(String field, String operator, int argIndex) {
            this.field = field;
            this.operator = operator;
            this.argIndex = argIndex;
        }

        public String getField() {
            return field;
        }

        public String getOperator() {
            return operator;
        }

        public int getArgIndex() {
            return argIndex;
        }


    }




}
