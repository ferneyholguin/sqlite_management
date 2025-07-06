# Actualizaciones para README.md

## 1. Agregar sección sobre búsqueda con operadores AND y OR

Agregar después de la sección "1.4 Búsqueda por Campo con Ordenamiento":

```markdown
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
```

## 2. Actualizar sección sobre consultas de actualización

Reemplazar la sección "3. Consultas de Actualización (Update Queries)" con:

```markdown
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
```