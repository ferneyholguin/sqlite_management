# SQLite Management Query Documentation

Este documento proporciona una guía completa sobre las consultas que se pueden crear utilizando el sistema de gestión SQLite.
Todas las consultas se definen a través de interfaces que extienden `DynamicQuery<T>`, donde `T` es el tipo de entidad sobre la que se realizan las consultas.

## Tipos de Consultas

### 1. Consultas de Búsqueda (Find Queries)

Las consultas de búsqueda permiten recuperar entidades de la base de datos.

#### 1.1 Búsqueda por Campo Específico

**Patrón:** `findBy[NombreCampo](Tipo valor)`

**Ejemplos:**
```java
// Buscar productos por nombre
List<Product> findByName(String name);

// Buscar productos por ID
Optional<Product> findById(int id);

// Buscar productos por precio
List<Product> findByPrice(double price);

// Buscar usuarios por email
Optional<User> findByEmail(String email);
```

#### 1.2 Búsqueda de Todas las Entidades

**Patrón:** `findAll()`

**Ejemplos:**
```java
// Buscar todos los productos
List<Product> findAll();

// Buscar todas las líneas
List<Line> findAll();
```

#### 1.3 Búsqueda con Ordenamiento

**Patrón:** `findAllOrderBy[NombreCampo][Asc|Desc]()`

**Ejemplos:**
```java
// Buscar todos los productos ordenados por nombre ascendente
List<Product> findAllOrderByNameAsc();

// Buscar todos los productos ordenados por nombre descendente
List<Product> findAllOrderByNameDesc();

// Buscar todos los productos ordenados por ID ascendente
List<Product> findAllOrderByIdAsc();

// Buscar todos los productos ordenados por precio descendente
List<Product> findAllOrderByPriceDesc();
```

#### 1.4 Búsqueda por Campo con Ordenamiento

**Patrón:** `findAllBy[CampoWhere]OrderBy[CampoOrder][|DAscesc](Tipo valorWhere)`

**Ejemplos:**
```java
// Buscar productos por nombre, ordenados por ID ascendente
List<Product> findAllByNameOrderByIdAsc(String name);

// Buscar productos por categoría, ordenados por precio descendente
List<Product> findAllByCategoryOrderByPriceDesc(String category);

// Buscar usuarios por rol, ordenados por fecha de registro ascendente
List<User> findAllByRoleOrderByRegistrationDateAsc(String role);
```

### 2. Consultas de Guardado (Save Queries)

Las consultas de guardado permiten insertar o actualizar entidades en la base de datos.

**Patrón:** `save(T entity)`

**Ejemplos:**
```java
// Guardar un producto
Product save(Product product);

// Guardar una línea
Line save(Line line);

// Guardar un usuario
User save(User user);
```

**Notas:**
- Si la entidad tiene un valor de clave primaria y existe en la base de datos, se actualizará.
- Si la entidad no tiene un valor de clave primaria o no existe en la base de datos, se insertará.
- Si la clave primaria es auto-incrementable, el valor generado se asignará automáticamente a la entidad devuelta.

### 3. Consultas de Actualización (Update Queries)

Las consultas de actualización permiten modificar entidades existentes en la base de datos.

#### 3.1 Actualización por Campo Específico

**Patrón:** `updateBy[NombreCampo](T entity, Tipo valorWhere)`

**Ejemplos:**
```java
// Actualizar un producto por ID
int updateById(Product product, int id);

// Actualizar un usuario por email
int updateByEmail(User user, String email);
```

#### 3.2 Actualización de Campo con Múltiples Condiciones

**Patrón:** `update[Campo]Where[Condicion1]And[Condicion2]...(Tipo valorCampo, Tipo valorCondicion1, Tipo valorCondicion2, ...)`

**Ejemplos:**
```java
// Actualizar el estado de productos por ubicación y categoría
int updateStateWhereLocationAndCategory(boolean state, String location, String category);

// Actualizar el precio de productos por nombre y proveedor
int updatePriceWhereNameAndSupplier(double price, String name, String supplier);

// Actualizar el rol de usuarios por departamento y antigüedad
int updateRoleWhereDepartmentAndSeniority(String role, String department, int seniority);
```

### 4. Consultas con Joins (Join Queries)

Las consultas con joins permiten recuperar entidades relacionadas. Estas se configuran mediante la anotación `@Join` en los campos de la entidad.

**Ejemplo de definición de entidad con join:**
```java
@Table(name = "lines")
public class Line {
    @Column(name = "id", isPrimaryKey = true, isAutoIncrement = true)
    private int id;
    
    @Column(name = "quantity")
    private int quantity;
    
    @Column(name = "product_id")
    private int productId;
    
    @Join(relationShip = Product.class, source = "id", targetName = "product_id")
    private Product product;
    
    // Getters y setters
}
```

**Consultas que automáticamente cargan las relaciones:**
```java
// Buscar todas las líneas (cada línea tendrá su producto relacionado cargado)
List<Line> findAll();

// Buscar líneas por ID (la línea tendrá su producto relacionado cargado)
Optional<Line> findById(int id);
```

## Ejemplos Completos de Interfaces de Consulta

### Ejemplo 1: ProductQuery

```java
public interface ProductQuery extends DynamicQuery<Product> {
    // Búsqueda básica
    List<Product> findByName(String name);
    Optional<Product> findById(int id);
    
    // Búsqueda con ordenamiento
    List<Product> findAllOrderByNameAsc();
    List<Product> findAllOrderByNameDesc();
    List<Product> findAllOrderByPriceDesc();
    
    // Búsqueda por campo con ordenamiento
    List<Product> findAllByNameOrderByIdAsc(String name);
    List<Product> findAllByCategoryOrderByPriceDesc(String category);
    
    // Guardado
    Product save(Product product);
    
    // Actualización
    int updatePriceWhereNameAndCategory(double price, String name, String category);
}
```

### Ejemplo 2: UserQuery

```java
public interface UserQuery extends DynamicQuery<User> {
    // Búsqueda básica
    Optional<User> findByEmail(String email);
    List<User> findByRole(String role);
    
    // Búsqueda con ordenamiento
    List<User> findAllOrderByNameAsc();
    List<User> findAllOrderByRegistrationDateDesc();
    
    // Búsqueda por campo con ordenamiento
    List<User> findAllByRoleOrderByNameAsc(String role);
    
    // Guardado
    User save(User user);
    
    // Actualización
    int updateStatusWhereEmailAndRole(boolean status, String email, String role);
}
```

## Notas Importantes

1. Los nombres de los métodos deben seguir exactamente los patrones descritos para que el sistema pueda interpretarlos correctamente.
2. Los nombres de los campos en los métodos deben coincidir con los nombres de los campos en la clase de entidad (no con los nombres de las columnas en la base de datos).
3. Las consultas con joins se manejan automáticamente cuando se recuperan entidades, siempre que las relaciones estén correctamente definidas con la anotación `@Join`.
4. Las consultas de guardado manejan automáticamente las relaciones, guardando primero las entidades relacionadas si es necesario.
5. Las consultas de actualización no actualizan las claves primarias.