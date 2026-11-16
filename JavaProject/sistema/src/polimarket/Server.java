package polimarket;
import com.sun.net.httpserver.HttpServer;
import polimarket.business.*;
import polimarket.contracts.Contracts.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class Server {
    public static void main(String[] args) throws Exception {
        Path file = Path.of(args.length > 0 ? args[0] : "data/polimarket.ser");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        Store store = new Store(file);
        IRRHH rrhh = new RecursosHumanos(store);
        IBodega bodega = new Bodega(store, rrhh);
        IVentas ventas = new Ventas(store, rrhh, bodega);
        IProveedores proveedores = new Proveedores(store, rrhh, bodega);
        IEntregas entregas = new Entregas(store, rrhh, ventas, bodega);
        HttpServer server;
        try { server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0); }
        catch (Exception e) { store.close(); throw e; }
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/", new AdaptadorHTTP(rrhh, bodega, ventas, proveedores, entregas, Path.of("clientes/web")));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1); executor.close();
            try { store.close(); } catch (Exception e) { e.printStackTrace(System.err); }
        }));
        server.start(); System.out.println("PoliMarket listo en http://127.0.0.1:" + port + " | Estado: " + file);
    }
}
