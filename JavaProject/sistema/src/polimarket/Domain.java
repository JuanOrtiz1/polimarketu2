package polimarket;

import java.io.Serializable;
import java.util.List;

/** Modelo inmutable; las asociaciones se conservan por identificador. */
public final class Domain {
    private Domain() {}
    public enum Rol { RRHH, VENDEDOR, BODEGA, ENTREGAS }
    public record Usuario(String id, String nombre, Rol rol, boolean autorizado) implements Serializable {}
    public record Cliente(String id, String nombre, String direccion) implements Serializable {}
    public record Venta(String id, String vendedorId, String clienteId, List<LineaVenta> lineas) implements Serializable {
        public Venta { lineas = List.copyOf(lineas); }
    }
    public record LineaVenta(String productoId, int cantidad) implements Serializable {}
    public record Producto(String id, String nombre, List<String> proveedores) implements Serializable {
        public Producto { proveedores = List.copyOf(proveedores); }
    }
    public record Existencia(String productoId, int fisicas, int reservadas) implements Serializable {
        public Existencia {
            if (reservadas < 0 || fisicas < reservadas || fisicas > 1_000_000)
                throw new BusinessException(409, "Saldo de inventario inválido o capacidad excedida");
        }
        public int disponibles() { return fisicas - reservadas; }
    }
    public record Proveedor(String id, String nombre) implements Serializable {}
    public record Compra(String id, String proveedorId, String productoId, int cantidad, String referencia) implements Serializable {}
    public record Entrega(String id, String ventaId, String destinatario, String direccion, String fecha) implements Serializable {}
    public record Movimiento(String id, String productoId, int cantidad, String origenTipo, String origenId) implements Serializable {}
}
