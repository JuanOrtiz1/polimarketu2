package polimarket.contracts;

import java.util.*;
import polimarket.Domain.*;

public final class Contracts {
    private Contracts() {}
    public interface IRRHH {
        List<Usuario> vendedores(String token);
        Usuario autorizar(String token, String vendedor, boolean activo);
        Usuario verificar(String token, Rol... roles);
    }
    public interface IBodega {
        List<Existencia> existencias(String token);
        void reservar(List<LineaVenta> lineas);
        void ingresar(String producto, int cantidad, String compra);
        void sacar(List<LineaVenta> lineas, String entrega);
    }
    public interface IProveedores {
        List<Map<String, Object>> reposicion(String token);
        Compra comprar(String token, String proveedor, String producto, int cantidad, String referencia);
    }
    public interface IVentas {
        Map<String, Object> catalogo(String token);
        Venta vender(String token, String cliente, List<LineaVenta> lineas);
        List<Map<String, Object>> pendientes(String token);
        Venta obtener(String venta);
    }
    public interface IEntregas {
        List<Map<String, Object>> pendientes(String token);
        Entrega despachar(String token, String venta);
    }
}
