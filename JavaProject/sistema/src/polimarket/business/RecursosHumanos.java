package polimarket.business;
import polimarket.*;
import polimarket.Domain.*;
import polimarket.contracts.Contracts.IRRHH;
import java.util.*;
import static polimarket.BusinessException.require;

public final class RecursosHumanos implements IRRHH {
    private final Store store;
    private static final Map<String,String> TOKENS = Map.of("demo-rrhh", "rh1", "demo-vendedor", "v1", "demo-bodega", "b1", "demo-entregas", "e1");
    public RecursosHumanos(Store store) { this.store = store; }
    public Usuario verificar(String token, Rol... roles) {
        return store.read(() -> {
            String id = TOKENS.get(token == null ? "" : token);
            Usuario u = store.state().usuarios.get(id);
            require(u != null, 401, "Identidad desconocida");
            require(u.autorizado() && Arrays.asList(roles).contains(u.rol()), 403, "Usuario inactivo o rol no autorizado");
            return u;
        });
    }
    public List<Usuario> vendedores(String token) {
        return store.read(() -> { verificar(token, Rol.RRHH); return store.state().usuarios.values().stream().filter(u -> u.rol() == Rol.VENDEDOR).toList(); });
    }
    public Usuario autorizar(String token, String vendedor, boolean activo) {
        return store.transaction(() -> {
            verificar(token, Rol.RRHH);
            Usuario u = store.state().usuarios.get(vendedor);
            require(u != null && u.rol() == Rol.VENDEDOR, 404, "Vendedor inexistente");
            Usuario updated = new Usuario(u.id(), u.nombre(), u.rol(), activo);
            store.state().usuarios.put(vendedor, updated); return updated;
        });
    }
}
