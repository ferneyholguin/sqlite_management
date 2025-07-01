package com.jef.sqlite.management.interfaces;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para definir consultas SQL personalizadas en métodos de interfaz.
 * 
 * Esta anotación permite a los desarrolladores especificar una consulta SQL personalizada que se ejecutará
 * cuando se llame al método anotado. Los parámetros del método se pasan como argumentos
 * a la consulta SQL en el orden en que aparecen en la firma del método.
 * 
 * El tipo de retorno del método debe ser:
 * - List<T>: Devuelve una lista de entidades que coinciden con la consulta
 * - Optional<T>: Devuelve un Optional que contiene la primera entidad que coincide con la consulta, o vacío si ninguna coincide
 * 
 * Ejemplo de uso:
 * <pre>
 * {@code
 * // Consulta que devuelve una lista de entidades
 * @SQLiteQuery(sql = "SELECT * FROM empleados WHERE departamento = ?")
 * List<Empleado> buscarEmpleadosPorDepartamento(String departamento);
 * 
 * // Consulta que devuelve una única entidad como Optional
 * @SQLiteQuery(sql = "SELECT * FROM empleados WHERE id = ?")
 * Optional<Empleado> buscarEmpleadoPorId(int id);
 * 
 * // Consulta con múltiples parámetros
 * @SQLiteQuery(sql = "SELECT * FROM empleados WHERE departamento = ? AND salario > ?")
 * List<Empleado> buscarEmpleadosPorDepartamentoYSalarioMinimo(String departamento, double salarioMinimo);
 * 
 * // Consulta con cláusula IN
 * @SQLiteQuery(sql = "SELECT * FROM empleados WHERE id IN (?)")
 * List<Empleado> buscarEmpleadosConIdsEn(String ids); // ids es una lista separada por comas, ej. "1,2,3"
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SQLiteQuery {

    /**
     * La consulta SQL a ejecutar.
     * 
     * La consulta puede incluir marcadores de posición (?) para parámetros, que serán reemplazados
     * con los valores de los parámetros del método en el orden en que aparecen en la firma del método.
     * 
     * @return La cadena de consulta SQL
     */
    String sql();

    /**
     * Indica si el método espera un resultado o no
     * 
     * Si es true, el método ejecuta la consulta y retorna un Optional o un List con los resultados, segun lo haya establecido el desarrollador.
     * Si es false, el método ejecutará la consulta y retorna true cuando esta haya finalizado.
     * 
     * @return true si el método debe esperar por el resultado, false en caso contrario
     */
    boolean captureResult() default true;

}
