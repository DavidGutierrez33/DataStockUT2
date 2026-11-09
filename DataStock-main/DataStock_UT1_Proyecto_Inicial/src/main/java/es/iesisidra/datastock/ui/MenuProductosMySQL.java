package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.bd.CategoriaDAO;
import es.iesisidra.datastock.bd.MovimientoDAO;
import es.iesisidra.datastock.bd.ProductoDAO;
import es.iesisidra.datastock.bd.ProveedorDAO;
import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.MovimientoStock;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.modelo.ProductoDetalle;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import es.iesisidra.datastock.validacion.ProductoInvalidoException;
import es.iesisidra.datastock.validacion.ValidadorProducto;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Opciones 7.4 a 7.8 del menú MySQL: productos, consultas, movimientos,
 * historial e importación desde el inventario en memoria.
 *
 * <p>Igual que en MenuMySQL, aquí no hay SQL: se piden los datos, se validan
 * con ValidadorProducto y se llama a las clases del paquete {@code bd}.</p>
 */
public class MenuProductosMySQL {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Inventario inventario;

    /** @param inventario inventario en memoria de la UT1 (origen de la importación 7.8). */
    public MenuProductosMySQL(Inventario inventario) {
        this.inventario = inventario;
    }

    // ------------------------------------------------------------------
    // 7.4 · RF-JDBC-03 · CRUD de productos
    // ------------------------------------------------------------------

    public void menuProductos() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE PRODUCTOS ----------");
            System.out.println("1. Dar de alta un producto");
            System.out.println("2. Consultar producto por código");
            System.out.println("3. Modificar un producto");
            System.out.println("4. Eliminar un producto");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            switch (opcion) {
                case 1 -> altaProducto();
                case 2 -> consultarPorCodigo();
                case 3 -> modificarProducto();
                case 4 -> eliminarProducto();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void altaProducto() {
        MenuMySQL.titulo("ALTA DE PRODUCTO");
        System.out.println("(Pulsa Intro sin escribir nada en cualquier campo para cancelar)");
        try {
            String codigo = pedirCodigo("Código: ");
            if (codigo == null) {
                MenuMySQL.cancelada();
                return;
            }
            // No se sobrescribe nunca un producto existente
            if (ProductoDAO.existeCodigo(codigo)) {
                ColoresConsola.error("El producto " + codigo + " ya existe en MySQL (código duplicado).");
                return;
            }
            String nombre = pedirNombre("Nombre: ", null);
            if (nombre == null) {
                MenuMySQL.cancelada();
                return;
            }
            BigDecimal precio = pedirPrecio("Precio (ej. 49.90): ", null);
            if (precio == null) {
                MenuMySQL.cancelada();
                return;
            }
            int stock = pedirStock("Stock inicial: ");
            if (stock < 0) {
                MenuMySQL.cancelada();
                return;
            }
            int categoriaId = pedirCategoria("ID de categoría: ", 0);
            if (categoriaId == 0) {
                MenuMySQL.cancelada();
                return;
            }
            int proveedorId = pedirProveedor("ID de proveedor: ", 0);
            if (proveedorId == 0) {
                MenuMySQL.cancelada();
                return;
            }
            int filas = ProductoDAO.insertar(new Producto(codigo, nombre, precio, stock, categoriaId, proveedorId));
            if (filas == 1) {
                ColoresConsola.ok("Producto " + codigo + " guardado correctamente.");
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void consultarPorCodigo() {
        MenuMySQL.titulo("CONSULTAR PRODUCTO");
        try {
            String codigo = pedirCodigo("Código (Intro para cancelar): ");
            if (codigo == null) {
                return;
            }
            ProductoDetalle p = ProductoDAO.buscarPorCodigo(codigo);
            if (p == null) {
                ColoresConsola.aviso("No existe ningún producto con el código " + codigo + ".");
            } else {
                mostrarFicha(p);
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void modificarProducto() {
        MenuMySQL.titulo("MODIFICAR PRODUCTO");
        try {
            String codigo = pedirCodigo("Código (Intro para cancelar): ");
            if (codigo == null) {
                MenuMySQL.cancelada();
                return;
            }
            ProductoDetalle actual = ProductoDAO.buscarPorCodigo(codigo);
            if (actual == null) {
                ColoresConsola.error("No existe ningún producto con el código " + codigo + ".");
                return;
            }
            mostrarFicha(actual);
            ColoresConsola.aviso("Pulsa Intro para conservar cada valor. El código y el stock no se cambian aquí "
                    + "(el stock se cambia en el menú de movimientos).");

            // Si se pulsa Intro, cada método devuelve el valor actual que le pasamos
            String nombre = pedirNombre("Nombre [" + actual.getNombre() + "]: ", actual.getNombre());
            BigDecimal precio = pedirPrecio("Precio [" + actual.getPrecio().toPlainString() + "]: ", actual.getPrecio());
            int categoriaId = pedirCategoria("ID de categoría [" + actual.getCategoriaId() + "]: ", actual.getCategoriaId());
            int proveedorId = pedirProveedor("ID de proveedor [" + actual.getProveedorId() + "]: ", actual.getProveedorId());

            boolean sinCambios = nombre.equals(actual.getNombre())
                    && precio.compareTo(actual.getPrecio()) == 0
                    && categoriaId == actual.getCategoriaId()
                    && proveedorId == actual.getProveedorId();
            if (sinCambios) {
                ColoresConsola.aviso("No has cambiado ningún dato.");
                return;
            }
            if (!LectorConsola.confirmar("[AVISO] ¿Guardar los cambios del producto " + codigo + "?")) {
                MenuMySQL.cancelada();
                return;
            }
            // Mantenemos el código y el stock que ya tenía
            Producto modificado = new Producto(codigo, nombre, precio, actual.getStock(), categoriaId, proveedorId);
            int filas = ProductoDAO.actualizar(modificado);
            if (filas == 1) {
                ColoresConsola.ok("Producto " + codigo + " actualizado.");
            } else {
                ColoresConsola.error("No se ha modificado ninguna fila: el producto " + codigo + " ya no existe.");
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void eliminarProducto() {
        MenuMySQL.titulo("ELIMINAR PRODUCTO");
        try {
            String codigo = pedirCodigo("Código (Intro para cancelar): ");
            if (codigo == null) {
                MenuMySQL.cancelada();
                return;
            }
            ProductoDetalle actual = ProductoDAO.buscarPorCodigo(codigo);
            if (actual == null) {
                ColoresConsola.error("No existe ningún producto con el código " + codigo + ".");
                return;
            }
            mostrarFicha(actual);
            if (!LectorConsola.confirmar("[AVISO] ¿Confirmas la eliminación?")) {
                MenuMySQL.cancelada();
                return;
            }
            int filas = ProductoDAO.eliminar(codigo);
            if (filas == 1) {
                ColoresConsola.ok("Producto " + codigo + " eliminado.");
            } else {
                ColoresConsola.error("No se ha borrado nada: el producto " + codigo + " ya no existía.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                // Nunca borramos movimientos para forzar el borrado
                ColoresConsola.error("No se puede eliminar: el producto tiene movimientos de stock registrados. "
                        + "Se conserva el historial.");
            } else {
                ColoresConsola.error(ConexionBD.explicarError(e));
            }
        }
    }

    // ------------------------------------------------------------------
    // 7.5 · RF-JDBC-04 · Consultas
    // ------------------------------------------------------------------

    public void menuConsultas() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE CONSULTAS ----------");
            System.out.println("1. Listar todos los productos");
            System.out.println("2. Buscar producto por código");
            System.out.println("3. Buscar productos por nombre");
            System.out.println("4. Consultar productos con stock inferior a un umbral");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            switch (opcion) {
                case 1 -> listarTodos();
                case 2 -> consultarPorCodigo();
                case 3 -> buscarPorNombre();
                case 4 -> stockInferior();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void listarTodos() {
        MenuMySQL.titulo("PRODUCTOS ALMACENADOS EN MYSQL");
        try {
            mostrarTabla(ProductoDAO.listarTodos(), "No hay productos guardados en MySQL.");
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void buscarPorNombre() {
        MenuMySQL.titulo("BUSCAR PRODUCTOS POR NOMBRE");
        String texto = LectorConsola.leerTexto("Texto a buscar: ").trim();
        if (texto.isEmpty()) {
            ColoresConsola.aviso("Debes escribir algún texto.");
            return;
        }
        try {
            mostrarTabla(ProductoDAO.buscarPorNombre(texto), "No hay productos cuyo nombre contenga \"" + texto + "\".");
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void stockInferior() {
        MenuMySQL.titulo("PRODUCTOS CON STOCK BAJO");
        int umbral;
        while (true) {
            umbral = LectorConsola.leerEntero("Mostrar productos con stock menor que: ");
            if (umbral >= 0) {
                break;
            }
            ColoresConsola.error("El umbral debe ser 0 o mayor.");
        }
        try {
            mostrarTabla(ProductoDAO.buscarStockInferior(umbral), "No hay productos con stock menor que " + umbral + ".");
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    // ------------------------------------------------------------------
    // 7.6 · RF-JDBC-05 · Movimientos (procedimiento almacenado)
    // ------------------------------------------------------------------

    public void menuMovimientos() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE MOVIMIENTOS ----------");
            System.out.println("1. Registrar una ENTRADA de stock");
            System.out.println("2. Registrar una SALIDA de stock");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> registrarMovimiento("ENTRADA");
                case 2 -> registrarMovimiento("SALIDA");
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void registrarMovimiento(String tipo) {
        MenuMySQL.titulo("REGISTRAR " + tipo);
        String codigo = null;
        try {
            codigo = pedirCodigo("Código del producto (Intro para cancelar): ");
            if (codigo == null) {
                MenuMySQL.cancelada();
                return;
            }
            ProductoDetalle p = ProductoDAO.buscarPorCodigo(codigo);
            if (p == null) {
                ColoresConsola.error("No existe ningún producto con el código " + codigo + ".");
                return;
            }
            System.out.println("Producto: " + p.getNombre() + " | Stock actual: " + p.getStock());

            int cantidad = MenuMySQL.pedirIdPositivo("Cantidad (entero mayor que 0, Intro para cancelar): ");
            if (cantidad == 0) {
                MenuMySQL.cancelada();
                return;
            }
            if (!LectorConsola.confirmar("[AVISO] ¿Confirmas la " + tipo + " de " + cantidad + " unidades de " + codigo + "?")) {
                MenuMySQL.cancelada();
                return;
            }

            // Si es una SALIDA mayor que el stock, no lo comprobamos aquí: lo comprueba el
            // procedimiento con SELECT ... FOR UPDATE, que bloquea la fila. Así vale aunque
            // otro usuario haya cambiado el stock desde que lo hemos mostrado.
            MovimientoDAO.registrar(codigo, tipo, cantidad);

            // Leemos lo que ha guardado el procedimiento para enseñarlo
            MovimientoStock m = MovimientoDAO.ultimoDeProducto(codigo);
            ColoresConsola.ok(tipo + " registrada correctamente.");
            if (m != null) {
                System.out.println("Movimiento nº " + m.getId() + ": " + m.getTipo() + ", cantidad " + m.getCantidad());
                System.out.println("Stock anterior " + m.getStockAnterior() + " -> posterior " + m.getStockPosterior());
            }
            ProductoDetalle despues = ProductoDAO.buscarPorCodigo(codigo);
            if (despues != null) {
                System.out.println("Stock actual en la tabla producto: " + despues.getStock());
            }
        } catch (SQLException e) {
            // Aquí llegan "Stock insuficiente", "Producto inexistente", etc. del procedimiento.
            // El procedimiento ya ha hecho ROLLBACK: ni el stock ni el historial han cambiado.
            ColoresConsola.error(ConexionBD.explicarError(e));
            mostrarStockSinCambios(codigo);
        }
    }

    /** Tras un error en un movimiento, enseña el stock para comprobar que no ha cambiado. */
    private void mostrarStockSinCambios(String codigo) {
        if (codigo == null) {
            return;
        }
        try {
            ProductoDetalle p = ProductoDAO.buscarPorCodigo(codigo);
            if (p != null) {
                System.out.println("El stock de " + codigo + " permanece en " + p.getStock() + ".");
            }
        } catch (SQLException e) {
            // Si ni siquiera podemos leerlo, ya se ha mostrado el error anterior
        }
    }

    // ------------------------------------------------------------------
    // 7.7 · RF-JDBC-06 · Historial (solo lectura)
    // ------------------------------------------------------------------

    public void menuHistorial() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE HISTORIAL ----------");
            System.out.println("1. Consultar todos los movimientos");
            System.out.println("2. Consultar movimientos de un producto");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> historialCompleto();
                case 2 -> historialProducto();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void historialCompleto() {
        MenuMySQL.titulo("HISTORIAL DE MOVIMIENTOS");
        try {
            List<MovimientoStock> movimientos = MovimientoDAO.listarTodos();
            if (movimientos.isEmpty()) {
                ColoresConsola.aviso("No hay movimientos registrados.");
            } else {
                mostrarMovimientos(movimientos);
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void historialProducto() {
        MenuMySQL.titulo("HISTORIAL DE UN PRODUCTO");
        try {
            String codigo = pedirCodigo("Código (Intro para cancelar): ");
            if (codigo == null) {
                return;
            }
            if (!ProductoDAO.existeCodigo(codigo)) {
                ColoresConsola.error("No existe ningún producto con el código " + codigo + ".");
                return;
            }
            List<MovimientoStock> movimientos = MovimientoDAO.listarPorProducto(codigo);
            if (movimientos.isEmpty()) {
                ColoresConsola.aviso("Sin movimientos registrados para " + codigo + ".");
            } else {
                mostrarMovimientos(movimientos);
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    // ------------------------------------------------------------------
    // 7.8 · RF-JDBC-07 · Importar el inventario en memoria a MySQL
    // ------------------------------------------------------------------

    public void importarDesdeMemoria() {
        MenuMySQL.titulo("IMPORTAR PRODUCTOS DE MEMORIA A MYSQL");
        List<Producto> enMemoria = inventario.getProductos();
        System.out.println("Productos disponibles en memoria: " + enMemoria.size());
        if (enMemoria.isEmpty()) {
            ColoresConsola.aviso("No hay productos en memoria para importar. Importa antes un JSON o XML (opciones 2 y 3).");
            return;
        }
        if (!LectorConsola.confirmar("[AVISO] ¿Deseas importar estos productos a MySQL?")) {
            MenuMySQL.cancelada();
            return;
        }

        List<Producto> aceptados = new ArrayList<>();
        int omitidos = 0;
        int rechazados = 0;
        int insertados = 0;
        boolean loteEmpezado = false;
        boolean huboError = false;

        try {
            // 1) Validamos uno a uno. Los rechazados no se intentan insertar.
            for (Producto p : enMemoria) {
                System.out.println("\nComprobando " + p.getCodigo() + " - " + p.getNombre() + "...");
                try {
                    ValidadorProducto.validar(p);
                } catch (ProductoInvalidoException e) {
                    ColoresConsola.error("Datos incorrectos: " + e.getMessage() + ". Producto rechazado.");
                    rechazados++;
                    continue;
                }
                if (ProductoDAO.existeCodigo(p.getCodigo())) {
                    ColoresConsola.aviso("El producto ya existe en MySQL. Se omite.");
                    omitidos++;
                    continue;
                }
                // No creamos categorías ni proveedores: el JSON solo trae el id, no el resto de datos
                if (!CategoriaDAO.existeId(p.getCategoriaId())) {
                    ColoresConsola.error("No existe la categoría con ID " + p.getCategoriaId()
                            + ". El producto no se puede importar.");
                    rechazados++;
                    continue;
                }
                if (!ProveedorDAO.existeId(p.getProveedorId())) {
                    ColoresConsola.error("No existe el proveedor con ID " + p.getProveedorId()
                            + ". El producto no se puede importar.");
                    rechazados++;
                    continue;
                }
                ColoresConsola.ok("Producto válido para importar.");
                aceptados.add(p);
            }

            // Solo para la prueba CP-JDBC-18: con -Ddatastock.forzarFallo=true se repite el
            // primer producto al final del lote y así MySQL da un error de clave duplicada.
            if (Boolean.getBoolean("datastock.forzarFallo") && !aceptados.isEmpty()) {
                ColoresConsola.aviso("MODO PRUEBA: se repite " + aceptados.get(0).getCodigo()
                        + " al final del lote para forzar un error SQL.");
                aceptados.add(aceptados.get(0));
            }

            // 2) Insertamos todos los aceptados en una única transacción
            if (!aceptados.isEmpty()) {
                loteEmpezado = true;
                insertados = ProductoDAO.insertarLote(aceptados);
            }
        } catch (SQLException e) {
            ColoresConsola.error("Error SQL durante la importación: " + ConexionBD.explicarError(e));
            huboError = true;
            insertados = 0;
        }

        System.out.println("\n------------------------------------------------");
        System.out.println("RESULTADO DE LA IMPORTACIÓN");
        System.out.println("------------------------------------------------");
        System.out.printf("Productos analizados:              %d%n", enMemoria.size());
        System.out.printf("Productos insertados:              %d%n", insertados);
        System.out.printf("Omitidos por código duplicado:     %d%n", omitidos);
        System.out.printf("Rechazados por datos incorrectos:  %d%n", rechazados);
        if (huboError && loteEmpezado) {
            // insertarLote ya ha hecho rollback antes de lanzar la excepción
            ColoresConsola.error("Importación cancelada con rollback: no se ha confirmado ninguna inserción del lote.");
        } else if (huboError) {
            ColoresConsola.error("Importación cancelada antes de insertar: no se ha guardado ningún producto.");
        } else {
            ColoresConsola.ok("Importación finalizada.");
        }
        System.out.println("El inventario en memoria no se ha modificado.");
        LectorConsola.pausar();
    }

    // ------------------------------------------------------------------
    // Pedir datos de un producto (cada uno se valida al escribirlo)
    // ------------------------------------------------------------------

    /** @return código normalizado y válido, o null si se deja vacío. */
    private String pedirCodigo(String mensaje) {
        while (true) {
            String codigo = ValidadorProducto.normalizarCodigo(LectorConsola.leerTexto(mensaje));
            if (codigo == null || codigo.isEmpty()) {
                return null;
            }
            try {
                ValidadorProducto.validarCodigo(codigo);
                return codigo;
            } catch (ProductoInvalidoException e) {
                ColoresConsola.error("Código no válido: " + e.getMessage() + ".");
            }
        }
    }

    /** @return nombre válido; si se deja vacío devuelve valorActual (null en el alta = cancelar). */
    private String pedirNombre(String mensaje, String valorActual) {
        while (true) {
            String nombre = LectorConsola.leerTexto(mensaje).trim();
            if (nombre.isEmpty()) {
                return valorActual;
            }
            try {
                ValidadorProducto.validarNombre(nombre);
                return nombre;
            } catch (ProductoInvalidoException e) {
                ColoresConsola.error("Nombre no válido: " + e.getMessage() + ".");
            }
        }
    }

    /** @return precio válido; si se deja vacío devuelve valorActual. Admite coma o punto decimal. */
    private BigDecimal pedirPrecio(String mensaje, BigDecimal valorActual) {
        while (true) {
            String texto = LectorConsola.leerTexto(mensaje).trim().replace(',', '.');
            if (texto.isEmpty()) {
                return valorActual;
            }
            try {
                BigDecimal precio = new BigDecimal(texto);
                ValidadorProducto.validarPrecio(precio);
                return precio;
            } catch (NumberFormatException e) {
                ColoresConsola.error("El precio debe ser un número (ej. 49.90).");
            } catch (ProductoInvalidoException e) {
                ColoresConsola.error("Precio no válido: " + e.getMessage() + ".");
            }
        }
    }

    /** @return stock 0 o mayor, o -1 si se deja vacío (cancelar). */
    private int pedirStock(String mensaje) {
        while (true) {
            String texto = LectorConsola.leerTexto(mensaje).trim();
            if (texto.isEmpty()) {
                return -1;
            }
            try {
                int stock = Integer.parseInt(texto);
                ValidadorProducto.validarStock(stock);
                return stock;
            } catch (NumberFormatException e) {
                ColoresConsola.error("El stock debe ser un número entero.");
            } catch (ProductoInvalidoException e) {
                ColoresConsola.error("Stock no válido: " + e.getMessage() + ".");
            }
        }
    }

    /**
     * Pide un id de categoría que exista en MySQL (integridad referencial).
     *
     * @return el id, o valorActual si se deja vacío (0 en el alta = cancelar)
     */
    private int pedirCategoria(String mensaje, int valorActual) throws SQLException {
        while (true) {
            int id = MenuMySQL.pedirIdPositivo(mensaje);
            if (id == 0) {
                return valorActual;
            }
            if (CategoriaDAO.existeId(id)) {
                return id;
            }
            ColoresConsola.error("No existe la categoría con ID " + id
                    + ". Dala de alta antes en el menú de categorías o escribe otra.");
        }
    }

    /** Igual que pedirCategoria pero para proveedores. */
    private int pedirProveedor(String mensaje, int valorActual) throws SQLException {
        while (true) {
            int id = MenuMySQL.pedirIdPositivo(mensaje);
            if (id == 0) {
                return valorActual;
            }
            if (ProveedorDAO.existeId(id)) {
                return id;
            }
            ColoresConsola.error("No existe el proveedor con ID " + id
                    + ". Dalo de alta antes en el menú de proveedores o escribe otro.");
        }
    }

    // ------------------------------------------------------------------
    // Mostrar resultados
    // ------------------------------------------------------------------

    private void mostrarFicha(ProductoDetalle p) {
        System.out.println("Código:    " + p.getCodigo());
        System.out.println("Nombre:    " + p.getNombre());
        System.out.println("Precio:    " + p.getPrecio().toPlainString() + " €");
        System.out.println("Stock:     " + p.getStock());
        System.out.println("Categoría: " + p.getCategoriaId() + " - " + p.getNombreCategoria());
        System.out.println("Proveedor: " + p.getProveedorId() + " - " + p.getNombreProveedor());
    }

    private void mostrarTabla(List<ProductoDetalle> productos, String mensajeSiVacia) {
        if (productos.isEmpty()) {
            ColoresConsola.aviso(mensajeSiVacia);
            return;
        }
        System.out.printf("%-12s %-28s %10s %6s  %-18s %s%n", "CÓDIGO", "NOMBRE", "PRECIO", "STOCK", "CATEGORÍA", "PROVEEDOR");
        System.out.println("-------------------------------------------------------------------------------------------");
        for (ProductoDetalle p : productos) {
            System.out.printf("%-12s %-28s %8s € %6d  %-18s %s%n", p.getCodigo(), p.getNombre(),
                    p.getPrecio().toPlainString(), p.getStock(), p.getNombreCategoria(), p.getNombreProveedor());
        }
        System.out.println("Total de productos encontrados: " + productos.size());
    }

    private void mostrarMovimientos(List<MovimientoStock> movimientos) {
        System.out.printf("%-5s %-19s  %-12s %-8s %8s %9s %10s%n",
                "ID", "FECHA", "PRODUCTO", "TIPO", "CANTIDAD", "ANTERIOR", "POSTERIOR");
        System.out.println("-------------------------------------------------------------------------------");
        for (MovimientoStock m : movimientos) {
            System.out.printf("%-5d %-19s  %-12s %-8s %8d %9d %10d%n", m.getId(), m.getFecha().format(FORMATO_FECHA),
                    m.getProductoCodigo(), m.getTipo(), m.getCantidad(), m.getStockAnterior(), m.getStockPosterior());
        }
        System.out.println("Total de movimientos: " + movimientos.size());
    }
}
