package polimarket.business;
import polimarket.*;
import polimarket.Domain.*;
import polimarket.contracts.Contracts.*;
import java.util.*;
import java.time.Instant;

public final class Entregas implements IEntregas {
    private final Store store; private final IRRHH rrhh; private final IVentas ventas; private final IBodega bodega;
    public Entregas(Store store, IRRHH rrhh, IVentas ventas, IBodega bodega) { this.store=store; this.rrhh=rrhh; this.ventas=ventas; this.bodega=bodega; }
    public List<Map<String,Object>> pendientes(String token) {
        return store.read(() -> { rrhh.verificar(token, Rol.ENTREGAS); return ventas.pendientes(token); });
    }
    public Entrega despachar(String token, String venta) {
        return store.transaction(() -> {
            rrhh.verificar(token, Rol.ENTREGAS);
            Entrega existing = store.state().entregas.get(venta); if (existing != null) return existing;
            Venta v = ventas.obtener(venta); Cliente c = store.state().clientes.get(v.clienteId());
            Entrega e = new Entrega(Store.id(), venta, c.nombre(), c.direccion(), Instant.now().toString());
            bodega.sacar(v.lineas(), e.id()); store.state().entregas.put(venta, e); return e;
        });
    }
}
