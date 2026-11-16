package polimarket.business;
import polimarket.*;
import polimarket.Domain.*;
import polimarket.contracts.Contracts.*;
import java.util.*;
import static polimarket.BusinessException.require;

public final class Bodega implements IBodega {
    private final Store store; private final IRRHH rrhh;
    public Bodega(Store store, IRRHH rrhh) { this.store = store; this.rrhh = rrhh; }
    public List<Existencia> existencias(String token) {
        return store.read(() -> { rrhh.verificar(token, Rol.VENDEDOR, Rol.BODEGA, Rol.ENTREGAS); return List.copyOf(store.state().existencias.values()); });
    }
    private Existencia saldo(String producto) {
        Existencia e = store.state().existencias.get(producto);
        require(e != null, 404, "Producto inexistente: " + producto); return e;
    }
    public static void cantidad(int cantidad) { require(cantidad > 0 && cantidad <= 1_000_000, 400, "Cantidad debe ser entera entre 1 y 1000000"); }
    public void reservar(List<LineaVenta> lineas) {
        store.requireTransaction();
        for (LineaVenta l : lineas) { cantidad(l.cantidad()); require(saldo(l.productoId()).disponibles() >= l.cantidad(), 409, "Disponibilidad insuficiente: " + l.productoId()); }
        for (LineaVenta l : lineas) { Existencia e = saldo(l.productoId()); store.state().existencias.put(l.productoId(), new Existencia(l.productoId(), e.fisicas(), e.reservadas() + l.cantidad())); }
    }
    public void ingresar(String producto, int cantidad, String compra) {
        store.requireTransaction(); cantidad(cantidad); Existencia e = saldo(producto);
        store.state().existencias.put(producto, new Existencia(producto, e.fisicas() + cantidad, e.reservadas()));
        movimiento(producto, cantidad, "COMPRA", compra);
    }
    public void sacar(List<LineaVenta> lineas, String entrega) {
        store.requireTransaction();
        for (LineaVenta l : lineas) { cantidad(l.cantidad()); require(saldo(l.productoId()).reservadas() >= l.cantidad(), 409, "Reservas inconsistentes"); }
        for (LineaVenta l : lineas) {
            Existencia e = saldo(l.productoId());
            store.state().existencias.put(l.productoId(), new Existencia(l.productoId(), e.fisicas()-l.cantidad(), e.reservadas()-l.cantidad()));
            movimiento(l.productoId(), -l.cantidad(), "ENTREGA", entrega);
        }
    }
    private void movimiento(String producto, int cantidad, String tipo, String origen) {
        String id = Store.id(); store.state().movimientos.put(id, new Movimiento(id, producto, cantidad, tipo, origen));
    }
}
