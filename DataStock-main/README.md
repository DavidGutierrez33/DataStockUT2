# DataStock – UT1 + UT2 · Ficheros y base de datos MySQL con JDBC

**Módulo:** Acceso a Datos · 2.º DAM · IES Isidra de Guzmán · Curso 2026–2027
**Autor:** David Gutiérrez
**Lenguaje:** Java 21 · **Construcción:** Maven · **Interfaz:** consola

DataStock es una aplicación de consola para gestionar el inventario de una pequeña empresa de artículos informáticos.

- **UT1:** ficheros (JSON, XML, serialización, XPath, `RandomAccessFile`) y un inventario **en memoria**.
- **UT2:** persistencia en **MySQL** con JDBC: categorías, proveedores, productos, movimientos de stock con procedimiento almacenado, transacciones y una actividad comparativa con SQLite.

## 1. Requisitos

- JDK **21** y Maven 3.8 o superior (o un IDE con Maven).
- **MySQL 8.0.16 o posterior** (hace falta por las restricciones `CHECK`) y MySQL Workbench o similar.
- Conexión a Internet la primera vez para que Maven descargue las dependencias.
- Consola con UTF-8 para ver bien tildes y el símbolo €.

| Dependencia | Versión | Para qué |
|---|---|---|
| `com.mysql:mysql-connector-j` | **9.3.0** | Driver JDBC de MySQL (tipo 4) |
| `org.xerial:sqlite-jdbc` | **3.46.1.0** | Driver JDBC de SQLite (solo `DemoSQLite`) |
| `com.google.code.gson:gson` | 2.11.0 | Exportar JSON (UT1) |
| `org.json:json` | 20240303 | Importar JSON (UT1) |

## 2. Preparar la base de datos MySQL

1. Abre `material_referencia/DataStock_BD_MySQL.sql` en MySQL Workbench y ejecútalo entero.
   Crea la base `datastock`, las tablas `categoria`, `proveedor`, `producto`, `movimiento_stock` y el procedimiento `registrar_movimiento`.
   El script no borra datos: se puede ejecutar varias veces.
2. Comprueba que existen las cuatro tablas y el procedimiento:

```sql
USE datastock;
SHOW TABLES;
SHOW PROCEDURE STATUS WHERE Db = 'datastock';
```

No se han modificado ni eliminado las restricciones del script (PK, UNIQUE, NOT NULL, CHECK y FK).

## 3. Configurar la conexión (sin publicar contraseñas)

Los datos de conexión se leen de un fichero **`db.properties`** con la clase `Properties` (clase `config/ConexionBD`).

1. Copia la plantilla:

```bash
cp src/main/resources/db.properties.example src/main/resources/db.properties
```

2. Edita `src/main/resources/db.properties` con tus datos de MySQL local:

```properties
jdbc.url=jdbc:mysql://localhost:3306/datastock
jdbc.user=alumno
jdbc.password=TU_CLAVE_LOCAL
```

- `db.properties` está en `.gitignore`: **no se sube al repositorio**. Solo se sube la plantilla `.example`, sin contraseña real.
- Maven copia el fichero a `target/classes`. Si lo cambias (por ejemplo, para la prueba de contraseña incorrecta), vuelve a ejecutar `mvn compile` antes de lanzar la aplicación.
- La contraseña nunca se muestra por consola: si la conexión falla se muestra un mensaje explicando la causa (servidor apagado, credenciales incorrectas o base de datos inexistente).

## 4. Cómo ejecutar

Desde la carpeta que contiene `pom.xml`:

```bash
mvn clean compile
mvn exec:java
```

Programas independientes (no están en el menú):

```bash
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.XPathActividad
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.AccesoAleatorioActividad
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.DemoSQLite
```

Si el IDE muestra caracteres raros de color (secuencias ANSI), ejecuta con `-Ddatastock.color=false` o define `NO_COLOR=1`. Los mensajes siguen entendiéndose por sus prefijos `[OK]`, `[ERROR]` y `[AVISO]`.

## 5. Menús

### Menú principal

| Opción | Operación | Almacén |
|---|---|---|
| 1 | Explorar ficheros y directorios | Ficheros |
| 2 | Importar productos desde JSON | Memoria |
| 3 | Importar productos desde XML | Memoria |
| 4 | Consultar productos en memoria | Memoria |
| 5 | Exportar productos (JSON / XML) | Memoria → fichero |
| 6 | Guardar y recuperar inventario `.dat` | Memoria ↔ fichero |
| **7** | **Gestionar base de datos MySQL** | **MySQL** |

### Menú 7 · Gestión MySQL (UT2)

| Opción | Operación | Requisito | JDBC utilizado |
|---|---|---|---|
| 7.1 | Comprobar conexión, nombre de la BD, versión y registros por tabla | RF-JDBC-01 | `DriverManager`, `DatabaseMetaData`, `Statement` |
| 7.2 | Categorías: alta, listado, modificar nombre, eliminar | RF-JDBC-02 | `PreparedStatement`, `executeUpdate()` |
| 7.3 | Proveedores: alta, listado, modificar nombre/email, eliminar | RF-JDBC-02 | `PreparedStatement`, `setNull` |
| 7.4 | Productos: alta, consulta por código, modificar, eliminar | RF-JDBC-03 | `PreparedStatement`, `setBigDecimal` |
| 7.5 | Consultas: listado con JOIN, por código, por nombre (`LIKE`), stock bajo un umbral | RF-JDBC-04 | `ResultSet` → objetos `ProductoDetalle` |
| 7.6 | Registrar ENTRADA / SALIDA de stock | RF-JDBC-05 | `CallableStatement` → `registrar_movimiento` |
| 7.7 | Historial completo o de un producto | RF-JDBC-06 | `ResultSet` → objetos `MovimientoStock` |
| 7.8 | Importar productos de memoria a MySQL | RF-JDBC-07 | Transacción: `setAutoCommit(false)`, `commit`, `rollback` |
| 7.9 | Demostración de commit y rollback | RF-JDBC-08 | Transacción con categorías de prueba |

Convenciones: los datos se piden uno a uno; **pulsar Intro sin escribir nada cancela** (en las modificaciones, conserva el valor actual). Antes de borrar o modificar se pide confirmación S/N. Una operación fallida muestra `[ERROR]` en rojo y vuelve al menú sin cerrar la aplicación.

## 6. Memoria y MySQL son almacenes distintos

- Las opciones 1–6 trabajan **solo** con la colección en memoria. Importar un JSON o XML **no** guarda nada en MySQL.
- Las consultas del menú 7 leen **siempre de MySQL**, nunca de la colección en memoria.
- La única conexión entre los dos es la opción **7.8**, que **copia** los productos de memoria a MySQL cuando el usuario lo pide. No borra ni cambia la memoria y no deja ninguna sincronización automática.
- Al cerrar DataStock, la memoria se pierde (salvo que se haya guardado un `.dat`); lo que está en MySQL permanece.

## 7. Reglas de validación y de integridad

Las reglas se comprueban en Java **antes** de ejecutar el SQL (`ValidadorProducto` y los menús) y, además, se capturan los errores que devuelva MySQL.

| Dato | Regla |
|---|---|
| Código de producto | 1–12 caracteres `A-Z`, `0-9` y `-`. Se pasa a mayúsculas (`tec-001` → `TEC-001`). Único. |
| Nombre de producto | 3–100 caracteres, sin espacios al principio ni al final |
| Precio | `BigDecimal` > 0, máximo 2 decimales, máximo 99999999.99 (`DECIMAL(10,2)`). Se admite coma o punto. |
| Stock | Entero ≥ 0. Solo se indica en el alta; después se cambia **únicamente** con movimientos. |
| Categoría / proveedor del producto | Entero positivo que **exista** en MySQL. No se crean automáticamente. |
| Categoría | ID entero positivo único; nombre 1–60 caracteres, único |
| Proveedor | ID entero positivo único; nombre 1–100; email opcional (máx. 120, formato básico, único) o `NULL` |

Borrados bloqueados por clave foránea (MySQL error 1451): una categoría o proveedor con productos, o un producto con movimientos. La aplicación lo explica y **no** borra los registros relacionados para forzarlo.

## 8. Transacciones

### Movimientos de stock (7.6)

Se llama a `registrar_movimiento(?, ?, ?)` con `CallableStatement`. **No** se abre otra transacción en Java: el procedimiento ya hace `START TRANSACTION`, `SELECT ... FOR UPDATE`, `UPDATE`, `INSERT`, `COMMIT` y `ROLLBACK` si falla. Si la salida supera el stock, el procedimiento lanza `SIGNAL` ("Stock insuficiente") y ni el stock ni el historial cambian.

### Importación desde memoria (7.8)

1. Se valida cada producto (datos, código duplicado en MySQL, categoría y proveedor existentes). Los duplicados se **omiten**; los incorrectos se **rechazan** con su motivo. Esto no impide importar los demás.
2. Los aceptados se insertan con un mismo `PreparedStatement` dentro de **una transacción**: `commit()` si todo va bien y `rollback()` si falla una inserción, de forma que no queda ninguna inserción parcial del lote.

**Prueba CP-JDBC-18 (fallo forzado):** para provocar un error SQL durante el lote, ejecuta

```bash
mvn exec:java -Ddatastock.forzarFallo=true
```

Con esta propiedad, la opción 7.8 añade al final del lote una copia del primer producto aceptado. MySQL devuelve un error de clave duplicada, se ejecuta `rollback()` y se puede comprobar en Workbench que no se ha guardado **ninguno** de los productos del lote.

### Demostración de commit/rollback (7.9)

- La aplicación busca el mayor `id` de `categoria` y usa los tres siguientes (por ejemplo, si el mayor es 2: **3, 4 y 5**). Así seguro que están libres y no tienen productos asociados.
- **Escenario A:** inserta las categorías `max+1` y `max+2` («Demo transacción N») en una transacción y hace `commit()`. Después las busca con `SELECT`.
- **Escenario B:** inserta la categoría `max+3` y repite el mismo id: error de clave duplicada → `rollback()`. Después comprueba que `max+3` **no** está en MySQL.
- Al terminar se restaura `autoCommit` y se cierra la conexión. La consola muestra los ids usados y ofrece borrar las dos categorías del escenario A. Para borrarlas a mano:

```sql
DELETE FROM categoria WHERE nombre LIKE 'Demo transacción %';
```

## 9. Actividad comparativa: SQLite (`DemoSQLite`)

Clase ejecutable independiente: `es.iesisidra.datastock.complementarias.DemoSQLite`. No está en los menús y DataStock sigue usando MySQL.

```bash
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.DemoSQLite
```

- Crea la carpeta `datos/sqlite/` y el fichero `datos/sqlite/productos_demo.db` (excluido de Git).
- Conecta con `DriverManager.getConnection("jdbc:sqlite:datos/sqlite/productos_demo.db")`.
- Crea `producto_demo` con `Statement`, inserta `SQL-001` y `SQL-002` con `PreparedStatement` e `INSERT OR IGNORE`, y los lista con `ResultSet`.
- En la segunda ejecución muestra `Insertados: 0 | Ya existentes: 2`: no se duplican filas.

### Comparativa MySQL y SQLite

| Pregunta | MySQL | SQLite |
|---|---|---|
| ¿Necesita servidor? | Sí, un servicio independiente (cliente-servidor) que hay que instalar y arrancar | No, es **embebido**: la librería va dentro de la aplicación |
| ¿Dónde se guardan los datos? | En los ficheros internos que gestiona el servidor | En un único fichero `.db` que elegimos nosotros |
| ¿Cómo se conecta por JDBC? | `jdbc:mysql://localhost:3306/datastock` + usuario y contraseña | `jdbc:sqlite:ruta/fichero.db`, sin usuario ni contraseña |
| ¿Qué dependencia se usa? | `com.mysql:mysql-connector-j` | `org.xerial:sqlite-jdbc` |
| Ventajas | Varios usuarios a la vez por red, permisos por usuario, procedimientos almacenados, más tipos y restricciones | No hay que instalar nada, muy ligero, el fichero se copia como cualquier otro |
| Inconvenientes | Hay que instalarlo, configurarlo y mantener el servidor encendido | Pensado para un solo programa; poca concurrencia de escritura; sin usuarios ni procedimientos almacenados |
| ¿Para qué lo elegiría? | Aplicaciones con varios usuarios o puestos que comparten datos (como DataStock en una empresa) | Apps de escritorio o móviles con datos locales, prototipos, pruebas, configuraciones |

**Pool de conexiones.** Abrir una conexión con MySQL cuesta tiempo (red, autenticación…). Un pool mantiene varias conexiones ya abiertas y las "presta": la aplicación coge una, la usa y la devuelve en lugar de cerrarla. Es útil en aplicaciones con muchos usuarios o peticiones a la vez, como un servidor web. DataStock no lo necesita: es una aplicación de consola de un solo usuario que hace pocas operaciones seguidas, así que cada operación abre una conexión corta, la usa y la cierra con try-with-resources. Así no se quedan conexiones abiertas mientras el usuario está en los menús.

## 10. Estructura del proyecto

```text
.
├── pom.xml
├── README.md
├── .gitignore
├── datos/
│   ├── ejemplos/               # Catálogos de entrada (incluye catalogo_ut2_recorrido.json)
│   ├── salidas/                # Exportaciones y binarios generados (ignorados por Git)
│   └── sqlite/                 # productos_demo.db de DemoSQLite (ignorado por Git)
├── material_referencia/        # DataStock_BD_MySQL.sql y diagrama E/R
├── docs/
│   └── pruebas_UT2.md          # Registro de pruebas CP-JDBC y CP-SQLITE
└── src/main/
    ├── resources/
    │   └── db.properties.example           # Plantilla de conexión (sin contraseña)
    └── java/es/iesisidra/datastock/
        ├── App.java
        ├── config/
        │   └── ConexionBD.java             # Abre conexiones leyendo db.properties
        ├── bd/                             # UT2: solo SQL/JDBC, sin consola
        │   ├── CategoriaDAO.java
        │   ├── ProveedorDAO.java
        │   ├── ProductoDAO.java            # CRUD, consultas con JOIN e importación en lote
        │   └── MovimientoDAO.java          # CallableStatement e historial
        ├── modelo/                         # Producto, ProductoDetalle, Categoria, Proveedor, MovimientoStock
        ├── servicio/Inventario.java        # Inventario en memoria (UT1)
        ├── validacion/                     # ValidadorProducto, ProductoInvalidoException
        ├── ficheros/                       # Importar/exportar/serializar (UT1)
        ├── ui/
        │   ├── MenuPrincipal.java
        │   ├── OperacionesInventario.java  # Opciones UT1
        │   ├── MenuMySQL.java              # 7.1, 7.2, 7.3 y 7.9
        │   └── MenuProductosMySQL.java     # 7.4 a 7.8
        ├── util/                           # LectorConsola, ColoresConsola, SelectorRutas
        └── complementarias/                # XPathActividad, AccesoAleatorioActividad, DemoSQLite
```

La consola (`ui`) está separada del acceso a datos (`bd`): los menús piden datos y muestran resultados; las clases `bd` solo ejecutan SQL y lanzan `SQLException`.

## 11. Recorrido de prueba (apartado 6.1 del enunciado)

1. Opción 2 → `datos/ejemplos/catalogo_ut2_recorrido.json` (3 productos en memoria).
2. 7.1 → conexión correcta, 0 productos en MySQL.
3. 7.2 → alta de categorías 1 «Periféricos» y 2 «Monitores».
4. 7.3 → alta del proveedor 1 «Distribuciones Norte» (no se da de alta el 3).
5. 7.8 → insertados 2, omitidos 0, rechazados 1 (MON-003: no existe el proveedor 3).
6. 7.5.1 → aparecen TEC-001 y RAT-002.
7. Salir, volver a ejecutar: la memoria está vacía y MySQL conserva los 2 productos.
8. 7.3 → alta del proveedor 3; opción 2 de nuevo; 7.8 → insertados 1, omitidos 2.
9. 7.6 → ENTRADA de 5 y SALIDA válida; SALIDA mayor que el stock → error, sin cambios; 7.7 → historial.

## 12. UT1: formatos y actividades

- **JSON:** objeto raíz `productos` con un array (también se acepta un array suelto). Campos `codigo`, `nombre`, `precio`, `stock`, `categoriaId`, `proveedorId`. Usa las mismas validaciones que el XML.
- **XML:** raíz `catalogo` con elementos `producto`. El lector (`XmlSeguro`) rechaza cualquier `DOCTYPE` (protección XXE).
- **Serialización:** `PersistenciaBinaria` usa un filtro de clases permitidas y valida cada producto leído.
- **XPath** (`XPathActividad`) y **acceso aleatorio** (`AccesoAleatorioActividad`, registros de 36 bytes en `stock.bin`) siguen disponibles como programas independientes.

## 13. Pruebas

Los casos CP-JDBC-01 a CP-JDBC-20 y CP-SQLITE-01 a CP-SQLITE-03 están en [`docs/pruebas_UT2.md`](docs/pruebas_UT2.md), con datos, resultado esperado, resultado obtenido y evidencia.
