# Registro de pruebas · DataStock UT2

**Alumno:** David Gutiérrez · **Versión:** etiqueta `UT2-v1.0`
**Entorno:** Java 21 · MySQL ______ · Connector/J 9.3.0 · sqlite-jdbc 3.46.1.0

Rellenar «Resultado obtenido», «Evidencia» (nº de captura) y «¿Superada?» después de ejecutar cada prueba.
Datos de partida: script `DataStock_BD_MySQL.sql` recién ejecutado y catálogo `datos/ejemplos/catalogo_ut2_recorrido.json`.

## Conexión (RF-JDBC-01)

| ID | Operación | Datos | Resultado esperado | Resultado obtenido | Evidencia | ¿Superada? |
|---|---|---|---|---|---|---|
| CP-JDBC-01 | 7.1 Comprobar conexión | `db.properties` correcto | `[OK]` verde, BD `datastock`, versión de MySQL y nº de productos | | | |
| CP-JDBC-02 | 7.1 con contraseña incorrecta | `jdbc.password` erróneo + `mvn compile` | `[ERROR]` rojo «Usuario o contraseña… incorrectos», sin mostrar la contraseña; vuelve al menú | | | |

## Categorías y proveedores (RF-JDBC-02)

| ID | Operación | Datos | Resultado esperado | Resultado obtenido | Evidencia | ¿Superada? |
|---|---|---|---|---|---|---|
| CP-JDBC-03 | 7.2.1 y 7.2.2 · 7.3.1 y 7.3.2 | Cat. 1 «Periféricos», 2 «Monitores»; prov. 1 «Distribuciones Norte» `ventas@distribuciones.example` | `[OK]` en cada alta; aparecen en los listados ordenados por ID | | | |
| CP-JDBC-04 | Alta duplicada y baja con productos | Cat. id 1 otra vez; nombre «monitores»; borrar cat. 1 con productos | `[ERROR]` por id y nombre duplicados; `[ERROR]` al borrar por productos asociados; nada se borra | | | |

## Productos (RF-JDBC-03)

| ID | Operación | Datos | Resultado esperado | Resultado obtenido | Evidencia | ¿Superada? |
|---|---|---|---|---|---|---|
| CP-JDBC-05 | 7.4.1 alta y 7.4.2 consulta | `cab-010`, «Cable HDMI», `9,95`, 30, cat. 1, prov. 1 | Se guarda como `CAB-010`; la consulta muestra todos los datos y los nombres de categoría y proveedor | | | |
| CP-JDBC-06 | 7.4.3 modificar | CAB-010: nombre «Cable HDMI 2m», precio `12.50`; Intro en categoría y proveedor | `[OK]`; en Workbench cambian nombre y precio, stock igual | | | |
| CP-JDBC-07 | Alta con código duplicado y categoría inexistente | `TEC-001`; luego otro código con categoría 99 | `[ERROR]` código duplicado; `[ERROR]` «No existe la categoría con ID 99» | | | |
| CP-JDBC-08 | 7.4.4 eliminar | CAB-010 (sin movimientos) → S; TEC-001 (con movimientos; hacer después de CP-JDBC-12) → S | CAB-010 eliminado `[OK]`; TEC-001 bloqueado `[ERROR]`, historial intacto | | | |

## Consultas (RF-JDBC-04)

| ID | Operación | Datos | Resultado esperado | Resultado obtenido | Evidencia | ¿Superada? |
|---|---|---|---|---|---|---|
| CP-JDBC-09 | 7.5.1 listar todos | — | Ordenado por código, con nombre de categoría y proveedor (JOIN) | | | |
| CP-JDBC-10 | 7.5.3 por nombre | `rat` y después `zzz` | Encuentra «Ratón inalámbrico»; con `zzz` aviso sin error | | | |
| CP-JDBC-11 | 7.5.4 umbral | Umbral `10` | Solo stock < 10 (MON-003 con 8; no TEC-001 con 10), ordenado por stock y código | | | |

## Movimientos (RF-JDBC-05)

| ID | Operación | Datos | Resultado esperado | Resultado obtenido | Evidencia | ¿Superada? |
|---|---|---|---|---|---|---|
| CP-JDBC-12 | 7.6.1 entrada | TEC-001, 5 | Stock anterior → posterior +5; nueva fila en `movimiento_stock` | | | |
| CP-JDBC-13 | 7.6.2 salida válida | TEC-001, 4 | Stock baja 4; movimiento SALIDA registrado | | | |
| CP-JDBC-14 | 7.6.2 salida excesiva | TEC-001, 200 | `[ERROR]` «Stock insuficiente»; stock igual; ningún movimiento nuevo | | | |

## Historial (RF-JDBC-06)

| ID | Operación | Datos | Resultado esperado | Resultado obtenido | Evidencia | ¿Superada? |
|---|---|---|---|---|---|---|
| CP-JDBC-15 | 7.7.1 y 7.7.2 | Todos; luego TEC-001 | Ordenados por fecha e ID; el filtro solo muestra TEC-001 | | | |
| CP-JDBC-16 | 7.7.2 sin movimientos | RAT-002; luego `NOEXISTE` | «Sin movimientos registrados»; para `NOEXISTE`, «No existe ningún producto» | | | |

## Importación desde memoria (RF-JDBC-07)

| ID | Operación | Datos | Resultado esperado | Resultado obtenido | Evidencia | ¿Superada? |
|---|---|---|---|---|---|---|
| CP-JDBC-17 | Opción 2 + 7.8 | `catalogo_ut2_recorrido.json`; cat. 1 y 2, solo prov. 1 | 1.ª vez: insertados 2, omitidos 0, rechazados 1 (prov. 3). Tras alta del prov. 3: insertados 1, omitidos 2 | | | |
| CP-JDBC-18 | 7.8 con fallo forzado | `mvn exec:java -Ddatastock.forzarFallo=true`, memoria con productos nuevos | `[ERROR]` de clave duplicada; mensaje de rollback; en Workbench no aparece ninguno del lote | | | |

## Transacción de demostración (RF-JDBC-08)

| ID | Operación | Datos | Resultado esperado | Resultado obtenido | Evidencia | ¿Superada? |
|---|---|---|---|---|---|---|
| CP-JDBC-19 | 7.9 escenario A | IDs mostrados: ___ y ___ | Commit; las dos categorías existen en MySQL | | | |
| CP-JDBC-20 | 7.9 escenario B | ID mostrado: ___ | Error de duplicado esperado; rollback; la categoría ___ no existe | | | |

## SQLite (actividad comparativa)

| ID | Operación | Datos | Resultado esperado | Resultado obtenido | Evidencia | ¿Superada? |
|---|---|---|---|---|---|---|
| CP-SQLITE-01 | 1.ª ejecución de `DemoSQLite` | Sin `datos/sqlite/` | Crea el fichero; «Insertados: 2 \| Ya existentes: 0»; lista SQL-001 y SQL-002 | | | |
| CP-SQLITE-02 | 2.ª ejecución | Fichero ya creado | «Insertados: 0 \| Ya existentes: 2»; siguen siendo 2 filas | | | |
| CP-SQLITE-03 | Cierre y persistencia | — | Mensaje de conexión cerrada; `productos_demo.db` sigue en disco | | | |
