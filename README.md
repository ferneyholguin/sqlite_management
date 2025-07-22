# SQLite Management

## Descripción
SQLite Management es una biblioteca para Android que simplifica la gestión de bases de datos SQLite. Proporciona una forma declarativa de definir tablas, columnas y relaciones utilizando anotaciones Java, y ofrece una API fluida para realizar consultas a la base de datos.

## Características
- Definición de tablas y columnas mediante anotaciones
- Soporte para relaciones entre tablas (joins)
- Consultas dinámicas basadas en nombres de métodos
- Consultas SQL personalizadas
- Manejo automático de transacciones
- Soporte para valores predeterminados y restricciones

## Instalación

### Gradle
Añade el repositorio JitPack a tu archivo `settings.gradle.kts` (para proyectos con Gradle Kotlin DSL) o `settings.gradle` (para proyectos con Gradle Groovy):

```gradle
// Para Gradle Kotlin DSL (settings.gradle.kts)
dependencyResolutionManagement {
    repositories {
        // Otros repositorios (Google, MavenCentral, etc.)
        maven { url = uri("https://jitpack.io") }
    }
}

// Para Gradle Groovy (settings.gradle)
dependencyResolutionManagement {
    repositories {
        // Otros repositorios (Google, MavenCentral, etc.)
        maven { url 'https://jitpack.io' }
    }
}
```

Luego, añade la dependencia a tu archivo `build.gradle.kts` (para Kotlin DSL) o `build.gradle` (para Groovy) del módulo:

```gradle
// Para Gradle Kotlin DSL (build.gradle.kts)
dependencies {
    implementation("com.github.ferneyholguin:sqlite_management:1.1.2")
}

// Para Gradle Groovy (build.gradle)
dependencies {
    implementation 'com.github.ferneyholguin:sqlite_management:1.1.2'
}
```

## Uso Básico

### 1. Definir una entidad

```java
@Table(name = "productos")
public class Producto {
    @Column(name = "id", primaryKey = true, autoIncrement = true)
    private int id;

    @Column(name = "nombre", unique = true, permitNull = false)
    private String nombre;

    @Column(name = "precio", defaultValue = "0.0")
    private double precio;

    @Column(name = "categoria_id")
    private int categoriaId;

    @Join(relationShip = Categoria.class, source = "id", targetName = "categoria_id")
    private Categoria categoria;

    // Getters y setters
}
```

### 2. Definir una interfaz de consulta

```java
public interface ProductoQuery extends DynamicQuery<Producto> {
    // Consultas de búsqueda básicas
    List<Producto> findByNombre(String nombre);
    Optional<Producto> findById(int id);

    // Consultas con ordenamiento
    List<Producto> findAllOrderByNombreAsc();
    List<Producto> findAllOrderByPrecioDesc();

    // Consultas combinadas
    List<Producto> findAllByCategoriaIdOrderByPrecioAsc(int categoriaId);

    // Consultas de guardado
    Producto save(Producto producto);

    // Consultas de actualización
    int updateByNombre(ContentValues values, String nombre);

    // Consulta SQL personalizada
    @SQLiteQuery(sql = "SELECT * FROM productos WHERE precio BETWEEN ? AND ?")
    List<Producto> findProductosEnRangoDePrecio(double precioMin, double precioMax);
}
```

### 3. Crear una tabla

```java
public class TablaProductos extends SQLiteTable<Producto> {
    public TablaProductos(SQLiteManagement management) {
        super(management);
    }

    // Métodos adicionales específicos para productos
}
```

### 4. Inicializar la base de datos

```java
public class MiBaseDeDatos extends SQLiteManagement {
    private static final String NOMBRE_BD = "mi_base_de_datos";
    private static final int VERSION_BD = 1;

    public MiBaseDeDatos(Context context) {
        super(context, NOMBRE_BD, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // La creación de tablas se maneja automáticamente por SQLiteTable
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Lógica de actualización de la base de datos
    }
}
```

### 5. Usar la biblioteca

```java
// Inicializar la base de datos
MiBaseDeDatos baseDeDatos = new MiBaseDeDatos(context);

// Crear una instancia de la tabla
TablaProductos tablaProductos = new TablaProductos(baseDeDatos);

// Crear una instancia de la interfaz de consulta
ProductoQuery productoQuery = QueryFactory.create(ProductoQuery.class, baseDeDatos);

// Guardar un producto
Producto producto = new Producto();
producto.setNombre("Smartphone XYZ");
producto.setPrecio(599.99);
Producto productoGuardado = productoQuery.save(producto);

// Buscar productos
List<Producto> todosLosProductos = productoQuery.findAll();
Optional<Producto> productoPorId = productoQuery.findById(1);
List<Producto> productosPorNombre = productoQuery.findByNombre("Smartphone XYZ");
List<Producto> productosOrdenadosPorPrecio = productoQuery.findAllOrderByPrecioDesc();

// Actualizar productos
ContentValues values = new ContentValues();
values.put("precio", 499.99);
int filasActualizadas = productoQuery.updateByNombre(values, "Smartphone XYZ");

// Consulta SQL personalizada
List<Producto> productosEnRango = productoQuery.findProductosEnRangoDePrecio(100.0, 1000.0);
```

## Tipos de Consultas

### 1. Consultas de Búsqueda (Find Queries)

#### 1.1 Búsqueda por Campo Específico

**Patrón:** `findBy[NombreCampo](Tipo valor)`

**Ejemplos:**
```java
// Buscar productos por nombre
List<Producto> findByNombre(String nombre);

// Buscar productos por ID
Optional<Producto> findById(int id);

// Buscar productos por precio
List<Producto> findByPrecio(double precio);

// Buscar usuarios por email
Optional<Usuario> findByEmail(String email);
```

#### 1.2 Búsqueda de Todas las Entidades

**Patrón:** `findAll()`

**Ejemplos:**
```java
// Buscar todos los productos
List<Producto> findAll();

// Buscar todas las categorías
List<Categoria> findAll();
```

#### 1.3 Búsqueda con Ordenamiento

**Patrón:** `findAllOrderBy[NombreCampo][Asc|Desc]()`

**Ejemplos:**
```java
// Buscar todos los productos ordenados por nombre ascendente
List<Producto> findAllOrderByNombreAsc();

// Buscar todos los productos ordenados por nombre descendente
List<Producto> findAllOrderByNombreDesc();

// Buscar todos los productos ordenados por ID ascendente
List<Producto> findAllOrderByIdAsc();

// Buscar todos los productos ordenados por precio descendente
List<Producto> findAllOrderByPrecioDesc();
```

#### 1.4 Búsqueda por Campo con Ordenamiento

**Patrón:** `findAllBy[CampoWhere]OrderBy[CampoOrder][Asc|Desc](Tipo valorWhere)`

**Ejemplos:**
```java
// Buscar productos por nombre, ordenados por ID ascendente
List<Producto> findAllByNombreOrderByIdAsc(String nombre);

// Buscar productos por categoría, ordenados por precio descendente
List<Producto> findAllByCategoriaOrderByPrecioDesc(String categoria);

// Buscar usuarios por rol, ordenados por fecha de registro ascendente
List<Usuario> findAllByRolOrderByFechaRegistroAsc(String rol);
```

#### 1.5 Búsqueda con Operadores AND y OR

**Patrón AND:** `findBy[Campo1]And[Campo2](Tipo valor1, Tipo valor2)`
**Patrón OR:** `findBy[Campo1]Or[Campo2](Tipo valor1, Tipo valor2)`

**Ejemplos:**
```java
// Buscar productos por nombre Y estado activo
List<Producto> findByNombreAndActivo(String nombre, boolean activo);

// Buscar productos por nombre O estado activo
List<Producto> findByNombreOrActivo(String nombre, boolean activo);

// Buscar usuarios por email Y rol
List<Usuario> findByEmailAndRol(String email, String rol);

// Buscar usuarios por nombre O apellido
List<Usuario> findByNombreOrApellido(String nombre, String apellido);
```

**Notas:**
- Las consultas con operador AND devuelven entidades que cumplen TODAS las condiciones.
- Las consultas con operador OR devuelven entidades que cumplen AL MENOS UNA de las condiciones.
- Se pueden combinar múltiples condiciones: `findBy[Campo1]And[Campo2]And[Campo3]` o `findBy[Campo1]Or[Campo2]Or[Campo3]`.
- También se pueden combinar con ordenamiento: `findBy[Campo1]And[Campo2]OrderBy[Campo3]Asc`.

### 2. Consultas de Guardado (Save Queries)

**Patrón:** `save(T entidad)`

**Ejemplos:**
```java
// Guardar un producto
Producto save(Producto producto);

// Guardar una categoría
Categoria save(Categoria categoria);

// Guardar un usuario
Usuario save(Usuario usuario);
```

**Notas:**
- Si la entidad tiene un valor de clave primaria y existe en la base de datos, se actualizará.
- Si la entidad no tiene un valor de clave primaria o no existe en la base de datos, se insertará.
- Si la clave primaria es auto-incrementable, el valor generado se asignará automáticamente a la entidad devuelta.

### 3. Consultas de Actualización (Update Queries)

#### 3.1 Actualización con ContentValues

**Patrón:** `updateBy[NombreCampo](ContentValues values, Tipo valorWhere)`

**Ejemplos:**
```java
// Actualizar un producto por ID
int updateById(ContentValues values, int id);

// Actualizar un usuario por email
int updateByEmail(ContentValues values, String email);

// Actualizar un producto por nombre
int updateByNombre(ContentValues values, String nombre);
```

**Notas:**
- El primer parámetro debe ser un objeto ContentValues que contiene los campos a actualizar y sus nuevos valores.
- El segundo parámetro (y siguientes, si hay más) son los valores para las condiciones WHERE.
- El método devuelve el número de filas actualizadas.

#### 3.2 Actualización de Campos Específicos

**Patrón:** `update[Campos]Where[Condiciones](Tipo valorCampo1, Tipo valorCampo2, ..., Tipo valorCondicion1, Tipo valorCondicion2, ...)`

**Ejemplos:**
```java
// Actualizar el nombre de un producto por ID
int updateNameWhereId(String name, int id);

// Actualizar el estado activo de un producto por ID
int updateActiveWhereId(boolean active, int id);

// Actualizar nombre y estado activo de un producto por ID
int updateNameActiveWhereId(String name, boolean active, int id);

// Actualizar nombre de productos por ID de línea
int updateNameWhereLineId(String name, int lineId);

// Actualizar nombre y estado activo de productos por nombre y ID de línea
int updateNameActiveWhereNameAndLineId(String newName, boolean newActive, String nameToMatch, int lineIdToMatch);
```

**Notas:**
- Los primeros parámetros son los valores para los campos a actualizar.
- Los últimos parámetros son los valores para las condiciones WHERE.
- El método devuelve el número de filas actualizadas.

#### 3.3 Actualización de Todas las Filas

**Patrón:** `update[Campos](Tipo valorCampo1, Tipo valorCampo2, ...)`

**Ejemplos:**
```java
// Actualizar el nombre de todos los productos
int updateName(String name);

// Actualizar el estado activo de todos los productos
int updateActive(boolean active);
```

**Notas:**
- Los parámetros son los valores para los campos a actualizar.
- No hay condiciones WHERE, por lo que se actualizan todas las filas.
- El método devuelve el número de filas actualizadas.

### 4. Consultas de Existencia (Exists Queries)

Las consultas de existencia permiten verificar si existen registros en la base de datos que cumplan con ciertos criterios, devolviendo un valor booleano.

#### 4.1 Verificación de Existencia por Campo Específico

**Patrón:** `existsBy[NombreCampo](Tipo valor)`

**Ejemplos:**
```java
// Verificar si existe un producto con un ID específico
boolean existsById(int id);

// Verificar si existe un producto con un nombre específico
boolean existsByNombre(String nombre);

// Verificar si existen productos activos
boolean existsByActivo(boolean activo);

// Verificar si existe un usuario con un email específico
boolean existsByEmail(String email);
```

#### 4.2 Verificación de Existencia con Múltiples Condiciones

**Patrón:** `existsBy[Condicion1]And[Condicion2]...(Tipo valorCondicion1, Tipo valorCondicion2, ...)`

**Ejemplos:**
```java
// Verificar si existe un producto con un nombre y estado específicos
boolean existsByNombreAndActivo(String nombre, boolean activo);

// Verificar si existe un producto con un precio y categoría específicos
boolean existsByPrecioAndCategoria(double precio, String categoria);

// Verificar si existe un usuario con un rol y departamento específicos
boolean existsByRolAndDepartamento(String rol, String departamento);
```

**Notas:**
- Las consultas de existencia devuelven `true` si se encuentra al menos un registro que cumpla con los criterios, y `false` en caso contrario.
- Estas consultas son útiles para validaciones rápidas sin necesidad de recuperar los datos completos.
- Internamente, estas consultas utilizan `COUNT(*)` para optimizar el rendimiento.

### 5. Consultas SQL Personalizadas

**Patrón:** `@SQLiteQuery(sql = "consulta SQL")`

**Ejemplos:**
```java
// Consulta personalizada con un parámetro
@SQLiteQuery(sql = "SELECT * FROM productos WHERE categoria_id = ?")
List<Producto> findProductosPorCategoria(int categoriaId);

// Consulta personalizada con múltiples parámetros
@SQLiteQuery(sql = "SELECT * FROM productos WHERE precio BETWEEN ? AND ?")
List<Producto> findProductosEnRangoDePrecio(double precioMin, double precioMax);

// Consulta personalizada que devuelve un solo resultado
@SQLiteQuery(sql = "SELECT * FROM usuarios WHERE email = ? AND password = ?")
Optional<Usuario> autenticarUsuario(String email, String password);
```

## Relaciones entre Tablas (Joins)

Las relaciones entre tablas se definen utilizando la anotación `@Join` en los campos de la entidad.

**Ejemplo de definición de entidad con join:**
```java
@Table(name = "lineas")
public class Linea {
    @Column(name = "id", primaryKey = true, autoIncrement = true)
    private int id;

    @Column(name = "cantidad")
    private int cantidad;

    @Column(name = "producto_id")
    private int productoId;

    @Join(relationShip = Producto.class, source = "id", targetName = "producto_id")
    private Producto producto;

    // Getters y setters
}
```

**Consultas que automáticamente cargan las relaciones:**
```java
// Buscar todas las líneas (cada línea tendrá su producto relacionado cargado)
List<Linea> findAll();

// Buscar líneas por ID (la línea tendrá su producto relacionado cargado)
Optional<Linea> findById(int id);
```

## Validación de Entidades

La biblioteca proporciona funcionalidad para validar entidades antes de insertarlas en la base de datos, asegurando que los datos sean correctos y cumplan con las restricciones definidas.

### Validación de Campos Obligatorios y Únicos

La biblioteca proporciona dos formas de validar entidades:

#### 1. Usando el método `validate` de la interfaz `DynamicQuery`

La interfaz `DynamicQuery` incluye un método `validate` que permite validar entidades directamente a través de la interfaz de consulta:

```java
// Crear un producto para validar
Producto producto = new Producto();
producto.setNombre("Smartphone XYZ");
producto.setPrecio(599.99);

try {
    // Validar el producto antes de guardarlo
    boolean esValido = productoQuery.validate(producto);
    if (esValido) {
        // El producto es válido, proceder a guardarlo
        productoQuery.save(producto);
    }
} catch (SQLiteException e) {
    // La validación falló, manejar el error
    System.err.println("Error de validación: " + e.getMessage());
}
```

#### 2. Usando la clase `QueryValidatorHandler` directamente

Para casos más específicos, también se puede utilizar la clase `QueryValidatorHandler` directamente:

```java
// Crear una instancia del validador
QueryValidatorHandler<Producto> validador = new QueryValidatorHandler<>(Producto.class, baseDeDatos);

// Crear un producto para validar
Producto producto = new Producto();
producto.setNombre("Smartphone XYZ");
producto.setPrecio(599.99);

try {
    // Validar el producto antes de guardarlo
    boolean esValido = validador.validateEntity(producto);
    if (esValido) {
        // El producto es válido, proceder a guardarlo
        productoQuery.save(producto);
    }
} catch (SQLiteException e) {
    // La validación falló, manejar el error
    System.err.println("Error de validación: " + e.getMessage());
}
```

En ambos casos, la validación verifica:

1. Que la entidad no sea nula
2. Que todos los campos marcados como no nulos (`permitNull = false`) tengan valores
3. Para campos únicos (`unique = true`), que no existan registros con el mismo valor en la base de datos
4. Que las relaciones requeridas (campos con anotación `@Join` y `permitNull = false`) estén presentes

### Definición de Restricciones en Entidades

Para que la validación funcione correctamente, es necesario definir las restricciones en los campos de la entidad:

```java
@Table(name = "productos")
public class Producto {
    @Column(name = "id", primaryKey = true, autoIncrement = true)
    private int id;

    @Column(name = "nombre", permitNull = false, unique = true)
    private String nombre;

    @Column(name = "precio", defaultValue = "0.0")
    private double precio;

    // Getters y setters
}
```

En este ejemplo:
- El campo `nombre` es obligatorio (`permitNull = false`) y debe ser único (`unique = true`)
- El campo `precio` tiene un valor predeterminado de 0.0

## Notas Importantes

1. Los nombres de los métodos deben seguir exactamente los patrones descritos para que el sistema pueda interpretarlos correctamente.
2. Los nombres de los campos en los métodos deben coincidir con los nombres de los campos en la clase de entidad (no con los nombres de las columnas en la base de datos).
3. Las consultas con joins se manejan automáticamente cuando se recuperan entidades, siempre que las relaciones estén correctamente definidas con la anotación `@Join`.
4. Las consultas de guardado manejan automáticamente las relaciones, guardando primero las entidades relacionadas si es necesario.
5. Las consultas de actualización no actualizan las claves primarias.
6. Es recomendable validar las entidades antes de guardarlas para evitar errores de restricción en la base de datos.

## Licencia

Este proyecto está licenciado bajo la [Licencia Apache 2.0](LICENSE).