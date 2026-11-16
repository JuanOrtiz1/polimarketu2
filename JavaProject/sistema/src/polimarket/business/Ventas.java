package polimarket.business;
import polimarket.*;
import polimarket.Domain.*;
import polimarket.contracts.Contracts.*;
import java.util.*;
import static polimarket.BusinessException.require;

public final class Ventas implements IVentas {
    private final Store store; private final IRRHH rrhh; private final IBodega bodega;
    public Ventas(Store store, IRRHH rrhh, IBodega bodega) { this.store=store; this.rrhh=rrhh; this.bodega=bodega; }
    public Map<String,Object> catalogo(String token) {
        return store.read(() -> { rrhh.verificar(token, Rol.VENDEDOR); return Map.of("clientes", List.copyOf(store.state().clientes.values()), "productos", List.copyOf(store.state().productos.values()), "existencias", bodega.existencias(token)); });
    }
    public Venta vender(String token, String cliente, List<LineaVenta> lineas) {
        return store.transaction(() -> {
            Usuario vendedor = rrhh.verificar(token, Rol.VENDEDOR);
            require(store.state().clientes.containsKey(cliente), 404, "Cliente inexistente");
            require(lineas != null && !lineas.isEmpty() && lineas.size() <= 100, 400, "Se requieren entre 1 y 100 líneas");
            Set<String> products = new HashSet<>();
            for (LineaVenta l : lineas) { require(l != null && products.add(l.productoId()), 400, "Producto duplicado o línea inválida"); Bodega.cantidad(l.cantidad()); }
            bodega.reservar(lineas);
            Venta venta = new Venta(Store.id(), vendedor.id(), cliente, lineas);
            store.state().ventas.put(venta.id(), venta); return venta;
        });
    }
    public List<Map<String,Object>> pendientes(String token) {
        return store.read(() -> { rrhh.verificar(token, Rol.ENTREGAS); return store.state().ventas.values().stream().filter(v -> !store.state().entregas.containsKey(v.id())).map(v -> Map.<String,Object>of("venta", v, "cliente", store.state().clientes.get(v.clienteId()))).toList(); });
    }
    public Venta obtener(String venta) {
        store.requireTransaction(); Venta v = store.state().ventas.get(venta); require(v != null, 404, "Venta inexistente"); return v;
    }
}
