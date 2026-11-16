package polimarket.business;
import polimarket.*;
import polimarket.Domain.*;
import polimarket.contracts.Contracts.*;
import java.util.*;
import static polimarket.BusinessException.require;

public final class Proveedores implements IProveedores {
    private final Store store; private final IRRHH rrhh; private final IBodega bodega;
    public Proveedores(Store store, IRRHH rrhh, IBodega bodega) { this.store=store; this.rrhh=rrhh; this.bodega=bodega; }
    public List<Map<String,Object>> reposicion(String token) {
        return store.read(() -> {
            rrhh.verificar(token, Rol.BODEGA);
            return store.state().productos.values().stream().map(p -> Map.<String,Object>of("producto", p, "proveedores", p.proveedores().stream().map(store.state().proveedores::get).toList())).toList();
        });
    }
    public Compra comprar(String token, String proveedor, String producto, int cantidad, String referencia) {
        return store.transaction(() -> {
            rrhh.verificar(token, Rol.BODEGA); Bodega.cantidad(cantidad);
            require(referencia != null && referencia.matches("[A-Za-z0-9_-]{1,60}"), 400, "Referencia inválida");
            Producto p = store.state().productos.get(producto);
            require(p != null, 404, "Producto inexistente");
            require(store.state().proveedores.containsKey(proveedor), 404, "Proveedor inexistente");
            require(p.proveedores().contains(proveedor), 409, "Proveedor no suministra el producto");
            String key = proveedor + ":" + referencia;
            Compra previous = store.state().compras.get(key);
            if (previous != null) {
                require(previous.productoId().equals(producto) && previous.cantidad() == cantidad, 409, "Referencia reutilizada con datos diferentes"); return previous;
            }
            Compra c = new Compra(Store.id(), proveedor, producto, cantidad, referencia);
            bodega.ingresar(producto, cantidad, c.id()); store.state().compras.put(key, c); return c;
        });
    }
}
