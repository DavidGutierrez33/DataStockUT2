package es.iesisidra.datastock.modelo;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Producto leído de MySQL junto con los NOMBRES de su categoría y su proveedor
 * (se obtienen con un JOIN).
 *
 * <p>Hereda de Producto para no repetir sus campos; solo añade los dos nombres.
 * Así cada fila del ResultSet se convierte en un objeto con campos identificados.</p>
 */
public class ProductoDetalle extends Producto {
    @Serial private static final long serialVersionUID = 1L;

    private String nombreCategoria;
    private String nombreProveedor;

    /** Constructor sin argumentos. */
    public ProductoDetalle() { }

    /** Construye el producto completo con los nombres de categoría y proveedor. */
    public ProductoDetalle(String codigo, String nombre, BigDecimal precio, int stock,
                           int categoriaId, int proveedorId,
                           String nombreCategoria, String nombreProveedor) {
        super(codigo, nombre, precio, stock, categoriaId, proveedorId);
        this.nombreCategoria = nombreCategoria;
        this.nombreProveedor = nombreProveedor;
    }

    /** @return nombre de la categoría del producto. */
    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }
    /** @return nombre del proveedor del producto. */
    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }

    @Override public String toString() {
        return "ProductoDetalle{" + super.toString() + ", categoria='" + nombreCategoria
                + "', proveedor='" + nombreProveedor + "'}";
    }
}
