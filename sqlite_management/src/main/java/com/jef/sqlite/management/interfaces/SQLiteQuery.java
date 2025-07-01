package com.jef.sqlite.management.interfaces;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining custom SQL queries on interface methods.
 * 
 * This annotation allows developers to specify a custom SQL query that will be executed
 * when the annotated method is called. The parameters of the method are passed as arguments
 * to the SQL query in the order they appear in the method signature.
 * 
 * The return type of the method must be either:
 * - List<T>: Returns a list of entities that match the query
 * - Optional<T>: Returns an Optional containing the first entity that matches the query, or empty if none match
 * 
 * Example usage:
 * <pre>
 * {@code
 * // Query returning a list of entities
 * @SQLiteQuery(sql = "SELECT * FROM employees WHERE department = ?")
 * List<Employee> findEmployeesByDepartment(String department);
 * 
 * // Query returning a single entity as Optional
 * @SQLiteQuery(sql = "SELECT * FROM employees WHERE id = ?")
 * Optional<Employee> findEmployeeById(int id);
 * 
 * // Query with multiple parameters
 * @SQLiteQuery(sql = "SELECT * FROM employees WHERE department = ? AND salary > ?")
 * List<Employee> findEmployeesByDepartmentAndMinSalary(String department, double minSalary);
 * 
 * // Query with IN clause
 * @SQLiteQuery(sql = "SELECT * FROM employees WHERE id IN (?)")
 * List<Employee> findEmployeesWithIdsIn(String ids); // ids is a comma-separated list, e.g. "1,2,3"
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SQLiteQuery {

    /**
     * The SQL query to execute.
     * 
     * The query can include placeholders (?) for parameters, which will be replaced
     * with the values of the method parameters in the order they appear in the method signature.
     * 
     * @return The SQL query string
     */
    String sql();

    boolean waitResult() default true;

}
